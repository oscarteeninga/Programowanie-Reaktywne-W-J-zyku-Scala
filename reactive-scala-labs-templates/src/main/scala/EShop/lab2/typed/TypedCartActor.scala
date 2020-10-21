package EShop.lab2.typed

import EShop.lab2.Cart
import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import scala.concurrent.duration._
import scala.language.postfixOps


class TypedCartActor {
  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer(context: ActorContext[TypedCommand]): Cancellable =
    context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def start: Behavior[TypedCommand] = empty

  def empty: Behavior[TypedCommand] = Behaviors.receive(
    (context, msg) => msg match {
      case AddItem(item) => nonEmpty(Cart.empty.addItem(item), scheduleTimer(context))
      case _ => Behaviors.same
    }
  )

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCommand] = Behaviors.receive(
    (context, msg) => msg match {
      case AddItem(item) => nonEmpty(cart.addItem(item), scheduleTimer(context))
      case RemoveItem(item) if cart.contains(item) =>
        if (cart.size > 1) nonEmpty(cart.removeItem(item), scheduleTimer(context)) else empty
      case StartCheckout =>
        inCheckout(cart)
      case StartWithCheckout(checkout) =>
        checkout ! StartCheckout
        inCheckout(cart)
      case ExpireCart => empty
      case _ => Behaviors.same
    }
  )

  def inCheckout(cart: Cart): Behavior[TypedCommand] = Behaviors.receive(
    (context, msg) => msg match {
      case ConfirmCheckoutCancelled => nonEmpty(cart, scheduleTimer(context))
      case ConfirmCheckoutClosed if cart.items.nonEmpty => empty
      case _ => Behaviors.same
    }
  )
}
