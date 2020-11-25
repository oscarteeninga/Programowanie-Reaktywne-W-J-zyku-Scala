package reactive5

import akka.actor._
import akka.event.LoggingReceive
import com.typesafe.config._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.language.postfixOps
import akka.actor.SupervisorStrategy.Restart

////////////////////////////////////////
// Bank account example with remoting //
////////////////////////////////////////

// Good practice: defining messages as case classes in an Actor's companion object
object BankAccount {
  case class Deposit(amount: BigInt) {
    require(amount > 0)
  }
  case class Withdraw(amount: BigInt) {
    require(amount > 0)
  }
  case object Done
  case object Init
}

class BankAccount extends Actor {
  import BankAccount._

  var balance = BigInt(0)

  def receive = LoggingReceive {
    case Deposit(amount) =>
      balance += amount
      println(s"Current balance: $balance")
      sender ! Done
    case Withdraw(amount) if amount <= balance =>
      balance -= amount
      println(s"Current balance: $balance")
      sender ! Done
    case Done =>
      context.system.terminate()
  }
}

object Client {
  case object Init
  case object Done
}

class Client extends Actor {
  import Client._
  import BankAccount.Deposit
  import BankAccount.Withdraw

  def receive = LoggingReceive { 
    case Init =>
      // use remote account
      // this actorSelection does not validate if the given actor really exists
      // use account.resolveOne() to validate
      val account =
        context.actorSelection("akka.tcp://Reactive5@127.0.0.1:2552/user/account")

      // use ask pattern
      implicit val timeout = Timeout(5 seconds)
      val future = account ? Deposit(200)
      // for demonstration only, one should not block in actor
      val result = Await.result(future, timeout.duration)

      sender ! Done
  }
}

object BankApp extends App {
  val config = ConfigFactory.load()
  val serversystem =
    ActorSystem("Reactive5", config.getConfig("serverapp").withFallback(config))
  val account = serversystem.actorOf(Props[BankAccount], "account")

  val clientsystem =
    ActorSystem("Reactive5", config.getConfig("clientapp").withFallback(config))
  val client = clientsystem.actorOf(Props[Client], "client")

  // use ask pattern
  implicit val timeout = Timeout(5 seconds)
  val future = client ? Client.Init
  val result = Await.result(future, timeout.duration)

  account ! BankAccount.Done

  clientsystem.terminate()

  Await.result(serversystem.whenTerminated, Duration.Inf)
  Await.result(clientsystem.whenTerminated, Duration.Inf)
}
