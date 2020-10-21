package EShop.lab2

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object Checkout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckOutClosed                   extends Event
  case class PaymentStarted(payment: ActorRef) extends Event

  def props(cart: ActorRef) = Props(new Checkout())
}

class Checkout extends Actor {

  import Checkout._

  private val scheduler = context.system.scheduler
  private val log = Logging(context.system, this)

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration = 1 seconds

  def receive: Receive = {
    case _ =>
      log.debug("[Init -> SelectingDelivery] Checkout started")
      context become selectingDelivery(scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout))
  }

  def selectingDelivery(timer: Cancellable): Receive = {
    case CancelCheckout =>
      log.debug("[SelectDelivery -> Cancelled] Checkout cancelled")
      context become cancelled
    case ExpireCheckout =>
      log.debug("[SelectDelivery -> Cancelled] Checkout timeout")
      context become cancelled
    case SelectDeliveryMethod(method) =>
      log.debug("[SelectDelivery -> SelectPayment] Delivery selected")
      context become selectingPaymentMethod(timer)
    case _ => log.warning("[SelectDelivery] Unexpected message")
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(payment) =>
      log.debug("[SelectPayment -> ProcessingPayment] Payment selected" + payment)
      context become processingPayment(scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment))
    case CancelCheckout =>
      log.debug("[SelectPayment -> Cancelled] Checkout cancelled")
      context become cancelled
    case ExpireCheckout =>
      log.debug("[SelectPayment -> Cancelled] Checkout timeout")
      context become cancelled
    case _ => log.warning("[SelectPayment] Unexpected message")
  }

  def processingPayment(timer: Cancellable): Receive = {
    case ConfirmPaymentReceived =>
      log.debug("[ProcessingPayment -> Closed] Payment confirmed")
      context become closed
    case CancelCheckout =>
      log.debug("[ProcessingPayment -> Cancelled] Payment cancelled")
      context become cancelled
    case ExpirePayment =>
      log.debug("[ProcessingPayment -> Cancelled] Payment timeout")
      context become cancelled
    case _ => log.warning("[ProcessingPayment] Unexpected message")
  }

  def cancelled: Receive = {
    case _ =>
      // sender ! ConfirmCheckoutCancelled
      log.debug("[Cancelled] Checkout cancelled")
      context.stop(self)
  }

  def closed: Receive = {
    case _ =>
      // sender ! ConfirmCheckoutClosed
      log.debug("[Closed] Checkout completed")
      context.stop(self)
  }
}
