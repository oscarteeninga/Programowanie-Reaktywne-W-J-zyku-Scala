package EShop.lab2.simple

import EShop.lab2._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Event
  case class PaymentStarted(payment: ActorRef) extends Event

  def props = Props(new Checkout)
}

class Checkout extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  var cart: ActorRef = ActorRef.noSender

  def receive: Receive = {
    case StartCheckout =>
      cart = sender
      log.debug("[Init -> SelectingDelivery] Checkout started")
      context become selectingDelivery(scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout))
    case StartTestCheckout =>
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
      log.debug("[SelectDelivery -> Cancelled] Checkout cancelled")
      context become cancelled
    case ExpireCheckout =>
      log.debug("[SelectDelivery -> Cancelled] Checkout timeout")
      context become cancelled
    case msg: Any => log.warning("[SelectDelivery] Unexpected message " + msg)
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
    case msg: Any => log.warning("[SelectPayment] Unexpected message " + msg)
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
    case msg: Any => log.warning("[ProcessingPayment] Unexpected message " + msg)
  }

  def cancelled: Receive = {
    case _ =>
      cart ! ConfirmCheckoutCancelled
      log.debug("[Cancelled] Checkout cancelled")
      context.stop(self)
  }

  def closed: Receive = {
    case _ =>
      cart ! ConfirmCheckoutClosed
      log.debug("[Closed] Checkout completed")
      context.stop(self)
  }
}
