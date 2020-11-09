package reactive4.persistence

import akka.actor.typed.{ActorSystem, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TypedPersistentBankAccount {
  trait Command
  case class Deposit(amount: BigInt) extends Command {
    require(amount > 0)
  }
  case class Withdraw(amount: BigInt) extends Command {
    require(amount > 0)
  }
  case object Print extends Command
  case object Done  extends Command

  trait Event
  case class BalanceChanged(delta: BigInt) extends Event

  case class AccountState(balance: BigInt = 0) {

    def updated(evt: BalanceChanged): AccountState = {
      println(s"Applying $evt")
      AccountState(balance + evt.delta)
    }

    override def toString: String = balance.toString
  }

  val commandHandler: (AccountState, Command) => Effect[Event, AccountState] = { (state, command) =>
    command match {
      case Deposit(amount) =>
        Effect.persist(BalanceChanged(amount))
      case Withdraw(amount) if amount <= state.balance =>
        Effect.persist(BalanceChanged(-amount))
      case Print =>
        println(s"Current balance: $state")
        Effect.none
      case Done =>
        Effect.stop()
    }
  }

  val eventHandler: (AccountState, Event) => AccountState = { (state, event) =>
    event match {
      case evt: BalanceChanged => state.updated(evt)
    }
  }

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior[Command, Event, AccountState](
      persistenceId = persistenceId,
      emptyState = AccountState(),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
//      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 6, keepNSnapshots = 2)) // please uncomment and try!

}

object TypedPersistentBankAccountMain extends App {

  import TypedPersistentBankAccount._

  val system: ActorSystem[TypedPersistentBankAccount.Command] =
    ActorSystem(TypedPersistentBankAccount(PersistenceId.ofUniqueId("account")), "account")

  system ! Deposit(1)
  system ! Deposit(2)

  system ! Deposit(3)
  system ! Withdraw(3)

  system ! Print

  system ! Done

  Await.result(system.whenTerminated, Duration.Inf)
}
