package EShop.lab4

import EShop.lab2.TypedCheckout
import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.concurrent.duration._

class TypedPersistentCartActor {

  import EShop.lab2.TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5.seconds

  private def scheduleTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def apply(persistenceId: PersistenceId): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId,
      Empty,
      commandHandler(context),
      eventHandler(context)
    )
  }

  def commandHandler(context: ActorContext[Command]): (State, Command) => Effect[Event, State] = (state, command) => {
    state match {
      case Empty =>
        command match {
          case AddItem(item) => Effect.persist(ItemAdded(item))
          case _             => Effect.none
        }
      case NonEmpty(cart, timer) =>
        command match {
          case AddItem(item) => Effect.persist(ItemAdded(item))
          case RemoveItem(item) =>
            if (cart.removeItem(item).items.isEmpty) Effect.persist(CartEmptied)
            else if (cart.contains(item)) Effect.persist(ItemRemoved(item))
            else Effect.none
          case StartCheckout(_) =>
            Effect.persist(CheckoutStarted(context.spawn(new TypedCheckout(context.self).start, "checkout")))
          case ExpireCart => Effect.persist(CartExpired)
        }
      case InCheckout(_) =>
        command match {
          case ConfirmCheckoutCancelled => Effect.persist(CheckoutCancelled)
          case ConfirmCheckoutClosed    => Effect.persist(CheckoutClosed)
          case _                        => Effect.none
        }
    }
  }

  def eventHandler(context: ActorContext[Command]): (State, Event) => State = (state, event) => {
    event match {
      case CheckoutStarted(_)        => InCheckout(state.cart)
      case ItemAdded(item)           => NonEmpty(state.cart.addItem(item), state.timerOpt.getOrElse(scheduleTimer(context)))
      case ItemRemoved(item)         => NonEmpty(state.cart.removeItem(item), state.timerOpt.get)
      case CartEmptied | CartExpired => Empty
      case CheckoutClosed            => Empty
      case CheckoutCancelled         => NonEmpty(state.cart, scheduleTimer(context))
    }
  }
}
