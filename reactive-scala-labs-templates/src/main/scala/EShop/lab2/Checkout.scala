package EShop.lab2

import EShop.lab2.Checkout._
import EShop.lab3.{OrderManager, Payment}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
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
      log.debug("[Init -> SelectingDelivery] Checkout started")
      context become selectingDelivery(scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout))
    case msg: Any =>
      log.warning("[Init] Unexpected message " + msg)
  }

  def selectingDelivery(timer: Cancellable): Receive = {
    case SelectDeliveryMethod(method) =>
      log.debug("[SelectDelivery -> SelectPayment] Delivery selected " + method)
      context become selectingPaymentMethod(timer)
    case CancelCheckout =>
      timer.cancel()
      log.debug("[SelectDelivery -> Cancelled] Checkout cancelled")
      context become cancelled
    case ExpireCheckout =>
      log.debug("[SelectDelivery -> Cancelled] Checkout timeout")
      context become cancelled
    case msg: Any => log.warning("[SelectDelivery] Unexpected message " + msg)
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(payment) =>
      timer.cancel()
      val paymentActor = context.actorOf(Payment.props(payment, sender, self))
      sender ! OrderManager.ConfirmPaymentStarted(paymentActor)
      log.debug("[SelectPayment -> ProcessingPayment] Payment selected" + payment)
      context become processingPayment(scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment))
    case CancelCheckout =>
      timer.cancel()
      log.debug("[SelectPayment -> Cancelled] Checkout cancelled")
      context become cancelled
    case ExpireCheckout =>
      cartActor ! CartActor.ConfirmCheckoutCancelled
      log.debug("[SelectPayment -> Cancelled] Checkout timeout")
      context become cancelled
    case msg: Any => log.warning("[SelectPayment] Unexpected message " + msg)
  }

  def processingPayment(timer: Cancellable): Receive = {
    case ConfirmPaymentReceived =>
      timer.cancel()
      log.debug("[ProcessingPayment -> Closed] Payment confirmed")
      cartActor ! CartActor.ConfirmCheckoutClosed
      context become closed
    case CancelCheckout =>
      timer.cancel()
      log.debug("[ProcessingPayment -> Cancelled] Payment cancelled")
      cartActor ! CartActor.ConfirmCheckoutCancelled
      context become cancelled
    case ExpirePayment =>
      log.debug("[ProcessingPayment -> Cancelled] Payment timeout")
      cartActor ! CartActor.ConfirmCheckoutCancelled
      context become cancelled
    case msg: Any => log.warning("[ProcessingPayment] Unexpected message " + msg)
  }

  def cancelled: Receive = _ => {
    log.debug("[Cancelled] Checkout cancelled")
    context.stop(self)
  }

  def closed: Receive = _ => {
    log.debug("[Closed] Checkout completed")
    context.stop(self)
  }
}
