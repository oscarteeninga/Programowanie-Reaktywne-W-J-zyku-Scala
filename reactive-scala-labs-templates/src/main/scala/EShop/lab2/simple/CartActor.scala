package EShop.lab2.simple

import EShop.lab2._
import akka.actor.{Actor, Cancellable, Props}
import akka.event.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {
  def props: Props = Props(new CartActor)
}

class CartActor extends Actor {

  private val log                       = Logging(context.system, this)
  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = empty

  def empty: Receive = {
    case AddItem(item) =>
      log.debug("[Empty -> NonEmpty] Item add")
      context become nonEmpty(Cart.empty.addItem(item), scheduleTimer)
    case ExpireCart =>
    case msg: Any =>
      log.warning("[Empty] Unexpected message " + msg)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
    case ExpireCart =>
      log.debug("[NonEmpty -> Empty] Expire cart by timeout")
      context become empty
    case AddItem(item) =>
      log.debug("[NonEmpty] Item add")
      context become nonEmpty(cart.addItem(item), timer)
    case RemoveItem(item) if cart.contains(item) =>
      if (cart.size == 1) {
        log.debug("[NonEmpty -> Empty] Item removed")
        context become empty
      } else {
        log.debug("[NonEmpty] Item removed")
        context become nonEmpty(cart.removeItem(item), timer)
      }
    case StartCheckout =>
      log.debug("[NonEmpty -> IsCheckout] Checkout started")
      context.actorOf(Checkout.props) ! CheckoutStarted(self)
      context become inCheckout(cart)
    case msg: Any => log.warning("[NonEmpty] Unexpected message " + msg)
  }

  def inCheckout(cart: Cart): Receive = {
    case ConfirmCheckoutCancelled =>
      log.debug("[IsCheckout -> NonEmpty] Checkout cancelled")
      context become nonEmpty(cart, scheduleTimer)
    case ConfirmCheckoutClosed =>
      log.debug("[IsCheckout -> Empty] Checkout closed")
      context become empty
    case msg: Any => log.warning("[IsCheckout] Unexpected message " + msg)
  }
}
