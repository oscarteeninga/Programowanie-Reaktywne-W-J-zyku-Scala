package EShop.lab4

import EShop.lab2.{Cart, Checkout}
import EShop.lab3.OrderManager
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object PersistentCartActor {

  def props(persistenceId: String) = Props(new PersistentCartActor(persistenceId))
}

class PersistentCartActor(
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5.seconds

  var state = Cart.empty

  var checkout: ActorRef = context.actorOf(Checkout.props(self))
  val orderManager       = context.parent

  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  override def receiveCommand: Receive = empty

  private def updateState(event: Event, timer: Option[Cancellable] = None): Unit = {
    event match {
      case CartExpired | CheckoutClosed =>
        state = Cart.empty
        context become empty
      case CheckoutCancelled(cart) =>
        state = cart
        context become nonEmpty(cart, scheduleTimer)
      case ItemAdded(item, cart) =>
        state = cart.addItem(item)
        context become nonEmpty(state, timer.getOrElse(scheduleTimer))
      case CartEmptied =>
        timer.map(_.cancel())
        state = Cart.empty
        context become empty
      case ItemRemoved(item, cart) =>
        state = cart.removeItem(item, timer.getOrElse(scheduleTimer))
        if (state.items.isEmpty)
          context become empty
        else
          context become nonEmpty(state, timer.getOrElse(scheduleTimer))
      case CheckoutStarted(checkoutRef, cart) =>
        timer.map(_.cancel())
        state = cart
        checkout = checkoutRef
        checkout ! Checkout.StartCheckout
        orderManager ! OrderManager.ConfirmCheckoutStarted(checkout)
        context become inCheckout(cart)
    }
  }

  def empty: Receive = {
    case AddItem(item) =>
      persist(ItemAdded(item, Cart.empty)) { updateState(_) }
    case GetItems =>
      sender ! Seq.empty
      context become empty
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
    case ExpireCart =>
      persist(CartExpired) { updateState(_) }
    case AddItem(item) =>
      persist(ItemAdded(item, cart)) { updateState(_, Some(timer)) }
    case RemoveItem(item) if cart.contains(item) =>
      persist(CartEmptied) { updateState(_, Some(timer)) }
    case GetItems =>
      sender ! cart.items
      context become nonEmpty(cart, timer)
    case StartCheckout =>
      persist(CheckoutStarted(checkout, cart)) { updateState(_, Some(timer)) }
  }

  def inCheckout(cart: Cart): Receive = {
    case ConfirmCheckoutCancelled =>
      persist(CheckoutCancelled(cart)) { updateState(_) }
    case ConfirmCheckoutClosed =>
      persist(CheckoutClosed) { updateState(_) }
    case GetItems =>
      sender ! cart.items
      context become inCheckout(cart)
  }

  override def receiveRecover: Receive = {
    case evt: Event                       => updateState(evt)
    case SnapshotOffer(_, snapshot: Cart) => state = snapshot
  }
}
