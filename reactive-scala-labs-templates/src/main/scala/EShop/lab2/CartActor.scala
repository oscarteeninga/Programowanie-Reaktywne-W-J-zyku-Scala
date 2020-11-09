package EShop.lab2

import EShop.lab3.OrderManager
import EShop.lab3.OrderManager.ConfirmCheckoutStarted
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command
  case object GetItems                 extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef, cart: Cart) extends Event
  case class ItemAdded(itemId: Any, cart: Cart)                 extends Event
  case class ItemRemoved(itemId: Any, cart: Cart)               extends Event
  case object CartEmptied                                       extends Event
  case object CartExpired                                       extends Event
  case object CheckoutClosed                                    extends Event
  case class CheckoutCancelled(cart: Cart)                      extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor {

  import CartActor._

  private val log                       = Logging(context.system, this)
  val cartTimerDuration: FiniteDuration = 5 seconds

  val checkout: ActorRef = context.actorOf(Checkout.props(self))
  val orderManager       = context.parent

  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = empty

  def empty: Receive = {
    case AddItem(item) =>
      log.debug("[Empty -> NonEmpty] Item add")
      context become nonEmpty(Cart.empty.addItem(item), scheduleTimer)
    case GetItems =>
      sender ! Seq.empty
      context become empty
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
        timer.cancel()
        log.debug("[NonEmpty -> Empty] Item removed")
        context become empty
      } else {
        log.debug("[NonEmpty] Item removed")
        context become nonEmpty(cart.removeItem(item), timer)
      }
    case GetItems =>
      sender ! cart.items
      context become nonEmpty(cart, timer)
    case StartCheckout =>
      timer.cancel()
      checkout ! Checkout.StartCheckout
      orderManager ! OrderManager.ConfirmCheckoutStarted(checkout)
      log.debug("[NonEmpty -> IsCheckout] Checkout started")
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
    case GetItems =>
      sender ! cart.items
      context become inCheckout(cart)
    case msg: Any => log.warning("[IsCheckout] Unexpected message " + msg)
  }
}
