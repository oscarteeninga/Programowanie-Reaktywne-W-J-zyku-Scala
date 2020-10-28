package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.language.postfixOps
import scala.concurrent.duration._

object TypedCartActor {

  sealed trait Command
  case class AddItem(item: Any)               extends Command
  case class RemoveItem(item: Any)            extends Command
  case object ExpireCart                      extends Command
  case object StartCheckout                   extends Command
  case object ConfirmCheckoutCancelled        extends Command
  case object ConfirmCheckoutClosed           extends Command
  case class GetItems(sender: ActorRef[Cart]) extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Event
}

class TypedCartActor {

  import TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer(context: ActorContext[TypedCartActor.Command]): Cancellable =
    context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def start: Behavior[TypedCartActor.Command] = empty

  def empty: Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(item) =>
          println("[Empty -> NonEmpty] Add item")
          nonEmpty(Cart.empty.addItem(item), scheduleTimer(context))
        case _ => Behaviors.same
    }
  )

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(item) =>
          println("[NonEmpty -> NonEmpty] Add item")
          nonEmpty(cart.addItem(item), scheduleTimer(context))
        case RemoveItem(item) if cart.contains(item) =>
          if (cart.size > 1) nonEmpty(cart.removeItem(item), scheduleTimer(context)) else empty
        case StartCheckout =>
          println("[NonEmpty -> inCheckout] Checkout started")
          inCheckout(cart)
        case ExpireCart =>
          println("[NonEmpty -> Empty] Cart expired")
          empty
        case _ => Behaviors.same
    }
  )

  def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ConfirmCheckoutCancelled =>
          println("[InCheckout -> NonEmpty] Checkout cancellation")
          nonEmpty(cart, scheduleTimer(context))
        case ConfirmCheckoutClosed if cart.items.nonEmpty =>
          println("[InCheckout -> Empty] Checkout closing")
          empty
        case _ => Behaviors.same
    }
  )
}
