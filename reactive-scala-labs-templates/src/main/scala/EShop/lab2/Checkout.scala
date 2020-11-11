package EShop.lab2

import EShop.lab2.Checkout._
import EShop.lab3.{OrderManager, Payment}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckOutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef)      extends Event
  case object CheckoutStarted                       extends Event
  case object CheckoutCancelled                     extends Event
  case class DeliveryMethodSelected(method: String) extends Event

  def props(cart: ActorRef) = Props(new Checkout(cart))
}

class Checkout(
  cartActor: ActorRef
) extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  def receive: Receive = {
    case StartCheckout =>
      context become selectingDelivery(scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout))
  }

  def selectingDelivery(timer: Cancellable): Receive = {
    case SelectDeliveryMethod(method) =>
      context become selectingPaymentMethod(timer)
    case CancelCheckout =>
      timer.cancel()
      context become cancelled
    case ExpireCheckout =>
      context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(payment) =>
      timer.cancel()
      val paymentActor = context.actorOf(Payment.props(payment, sender, self))
      sender ! OrderManager.ConfirmPaymentStarted(paymentActor)
      context become processingPayment(scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment))
    case CancelCheckout =>
      timer.cancel()
      cartActor ! CartActor.ConfirmCheckoutCancelled
      context become cancelled
    case ExpireCheckout =>
      cartActor ! CartActor.ConfirmCheckoutCancelled
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = {
    case ConfirmPaymentReceived =>
      timer.cancel()
      cartActor ! CartActor.ConfirmCheckoutClosed
      context become closed
    case CancelCheckout =>
      timer.cancel()
      cartActor ! CartActor.ConfirmCheckoutCancelled
      context become cancelled
    case ExpirePayment =>
      cartActor ! CartActor.ConfirmCheckoutCancelled
      context become cancelled
  }

  def cancelled: Receive = _ => {
    context stop self
  }

  def closed: Receive = _ => {
    context stop self
  }
}
