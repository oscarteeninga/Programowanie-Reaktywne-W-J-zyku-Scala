package reactive2

import akka.actor._
import akka.event.LoggingReceive
import scala.concurrent.Await
import scala.concurrent.duration._

////////////////////////////////////////
// More complex example: Bank account //
////////////////////////////////////////

/**
 *
 * Note:
 * 1. It is a good practice to define messages accepted by an actor as
 *    case classes of that Actor's companion object (a contract/API of this actor).
 * 2. It is important to distinguish different types of messages:
 *    - Command message: used to invoke an action in the target actor,
 *      e.g. "DoTransfer(...)"
 *    - Event message: used to inform that something happened in another actor,
 *      e.g. "TransferDone(...)"
 *    - Document message: used to pass information but without intent to invoke
 *      specific action, and without indication that something has happened.
 *      E.g. "MoneyTransfer(...)"
 * Read more: http://www.informit.com/articles/article.aspx?p=2428369
 *
 **/
// Messages of the BankAccount actor
object BankAccount {
  case class Deposit(amount: BigInt) {
    require(amount > 0)
  }
  case class Withdraw(amount: BigInt) {
    require(amount > 0)
  }
  case object Done
  case object Failed
  case object Init
}

class BankAccount extends Actor {
  import BankAccount._

  var balance = BigInt(0)

  def receive = LoggingReceive {
    case Deposit(amount) =>
      balance += amount
      sender ! Done
    case Withdraw(amount) if amount <= balance =>
      balance -= amount
      sender ! Done
    case _ => sender ! Failed
  }
}

// Wire transfers are handled by a separate actor

object WireTransfer {
  case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
  case object Done
  case object Failed
}

class WireTransfer extends Actor {
  import WireTransfer._

  // 1st step: we await transfer requests
  def receive = LoggingReceive {
    case Transfer(from, to, amount) =>
      from ! BankAccount.Withdraw(amount)
      context become awaitWithdraw(to, amount, sender)
  }

  // 2nd step: we await withdraw acknowledgment
  // we need to know the target account of the transfer, the amount to be transferred, and
  // the original sender of the transfer request (to send the final acknowledgment)
  def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive = LoggingReceive {
    case BankAccount.Done =>
      to ! BankAccount.Deposit(amount)
      context become awaitDeposit(client)

    case BankAccount.Failed =>
      client ! Failed
      context.stop(self)
  }

  // 3rd step: we await the deposit acknowledgment and notify the sender of the original request
  def awaitDeposit(customer: ActorRef): Receive = LoggingReceive {
    case BankAccount.Done =>
      customer ! Done
      context.stop(self)

  }
}

class Bank extends Actor {
  val account1 = context.actorOf(Props[BankAccount], "account1")
  val account2 = context.actorOf(Props[BankAccount], "account2")

  def receive = LoggingReceive {
    case BankAccount.Init => {
      account1 ! BankAccount.Deposit(100)
    }

    case BankAccount.Done => transfer(150)
  }

  def transfer(amount: BigInt): Unit = {
    val transaction = context.actorOf(Props[WireTransfer], "transfer")
    transaction ! WireTransfer.Transfer(account1, account2, amount)
    context.become(LoggingReceive {
      case WireTransfer.Done =>
        println("success")
        context.system.terminate
    })
  }
}

object BankApp extends App {
  val system    = ActorSystem("Reactive2")
  val mainActor = system.actorOf(Props[Bank], "mainActor")

  mainActor ! BankAccount.Init

  Await.result(system.whenTerminated, Duration.Inf)
}
