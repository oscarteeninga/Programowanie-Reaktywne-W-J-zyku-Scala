package reactive2

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Await
import scala.concurrent.duration._

////////////////////////////////////////
// More complex example: TypedBank account //
////////////////////////////////////////

// Messages of the TypedBankAccount actor
object TypedBankAccount {
  trait Command {
    def replyTo: ActorRef[TypedWireTransfer.Command]
  }
  case class Deposit(amount: BigInt, replyTo: ActorRef[TypedWireTransfer.Command]) extends Command {
    require(amount > 0)
  }
  case class Withdraw(amount: BigInt, replyTo: ActorRef[TypedWireTransfer.Command]) extends Command {
    require(amount > 0)
  }

  def apply(balance: BigInt): Behavior[Command] = Behaviors.receiveMessage {
    case Deposit(amount, replyTo) =>
      replyTo ! TypedWireTransfer.Done
      apply(balance + amount)
    case Withdraw(amount, replyTo) if amount <= balance =>
      replyTo ! TypedWireTransfer.Done
      apply(balance - amount)
    case c: Command =>
      c.replyTo ! TypedWireTransfer.Failed
      Behaviors.same
  }
}

// Wire transfers are handled by a separate actor

object TypedWireTransfer {
  trait Command
  case class Transfer(
    from: ActorRef[TypedBankAccount.Command],
    to: ActorRef[TypedBankAccount.Command],
    amount: BigInt,
    replyTo: ActorRef[TypedBank.Command]
  ) extends Command
  case object Done   extends Command
  case object Failed extends Command

  // 1st step: we await transfer requests
  def apply(): Behavior[Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case Transfer(from, to, amount, replyTo) =>
            from ! TypedBankAccount.Withdraw(amount, context.self)
            awaitWithdraw(to, amount, replyTo)
          case _ =>
            Behaviors.same
      }
    )

  // 2nd step: we await withdraw acknowledgment
  // we need to know the target account of the transfer, the amount to be transferred, and
  // the original sender of the transfer request (to send the final acknowledgment)
  def awaitWithdraw(
    to: ActorRef[TypedBankAccount.Command],
    amount: BigInt,
    client: ActorRef[TypedBank.Command]
  ): Behavior[Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case Done =>
            to ! TypedBankAccount.Deposit(amount, context.self)
            awaitDeposit(client)
          case Failed =>
            client ! TypedBank.Failed
            Behaviors.stopped
          case _ =>
            Behaviors.same
      }
    )

  // 3rd step: we await the deposit acknowledgment and notify the sender of the original request
  def awaitDeposit(customer: ActorRef[TypedBank.Command]): Behavior[Command] = Behaviors.receiveMessage {
    case Done =>
      customer ! TypedBank.Done
      Behaviors.stopped
    case _ =>
      Behaviors.same
  }
}

object TypedBank {

  trait Command
  case object Init   extends Command
  case object Done   extends Command
  case object Failed extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val account1 = context.spawn(TypedBankAccount(100), "account1")
    val account2 = context.spawn(TypedBankAccount(0), "account2")

    transfer(100, account1, account2)
  }

  def transfer(
    amount: BigInt,
    account1: ActorRef[TypedBankAccount.Command],
    account2: ActorRef[TypedBankAccount.Command]
  ): Behavior[Command] = Behaviors.setup { context =>
    val transaction = context.spawn(TypedWireTransfer(), "transfer")
    transaction ! TypedWireTransfer.Transfer(account1, account2, amount, context.self)
    Behaviors.receiveMessage {
      case Done =>
        println("success")
        context.system.terminate
        Behaviors.stopped
      case _ =>
        Behaviors.stopped
    }
  }

}

object TypedBankApp extends App {
  val system = ActorSystem(TypedBank(), "mainActor")

  Await.result(system.whenTerminated, Duration.Inf)
}
