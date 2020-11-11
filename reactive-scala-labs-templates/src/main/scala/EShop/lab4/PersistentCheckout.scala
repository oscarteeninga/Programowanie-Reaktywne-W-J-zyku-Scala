package EShop.lab4

import EShop.lab2.CartActor
import EShop.lab3.{OrderManager, Payment}
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.PersistentActor

import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object PersistentCheckout {

  def props(cartActor: ActorRef, persistenceId: String) =
    Props(new PersistentCheckout(cartActor, persistenceId))
}

class PersistentCheckout(
  cartActor: ActorRef,
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.Checkout._
  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)
  val timerDuration     = 1.seconds

  private def updateState(event: Event, timer: Option[Cancellable] = None): Unit = {
    event match {
      case CheckoutStarted =>
        context become selectingDelivery(scheduler.scheduleOnce(timerDuration, self, ExpireCheckout))
      case DeliveryMethodSelected(method) =>
        context become selectingPaymentMethod(
          timer.getOrElse(scheduler.scheduleOnce(timerDuration, self, ExpireCheckout))
        )
      case CheckOutClosed =>
        timer.foreach(_.cancel())
        context become closed
      case CheckoutCancelled =>
        timer.foreach(_.cancel())
        context become cancelled
      case PaymentStarted(payment) =>
        timer.foreach(_.cancel())
        context become processingPayment(scheduler.scheduleOnce(timerDuration, self, ExpirePayment))
    }
  }

  def receiveCommand: Receive = {
    case StartCheckout =>
      persist(CheckoutStarted) { updateState(_) }
  }

  def selectingDelivery(timer: Cancellable): Receive = {
    case SelectDeliveryMethod(method) =>
      persist(DeliveryMethodSelected(method)) { updateState(_, Some(timer)) }
    case CancelCheckout | ExpireCheckout =>
      persist(CheckoutCancelled) { updateState(_) }
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(payment) =>
      val paymentRef = context.actorOf(Payment.props(payment, sender, self))
      persist(PaymentStarted(paymentRef)) { updateState(_) }
    case CancelCheckout | ExpireCheckout =>
      persist(CheckoutCancelled) { updateState(_) }
  }

  def processingPayment(timer: Cancellable): Receive = {
    case ConfirmPaymentReceived =>
      persist(CheckOutClosed) { updateState(_) }
    case CancelCheckout | ExpireCheckout =>
      persist(CheckoutCancelled) { updateState(_) }
  }

  def cancelled: Receive = _ => {
    cartActor ! CartActor.ConfirmCheckoutCancelled
    context stop self
  }

  def closed: Receive = _ => {
    cartActor ! CartActor.ConfirmCheckoutClosed
    context stop self
  }

  override def receiveRecover: Receive = {
    case evt: Event => updateState(evt)
  }
}
