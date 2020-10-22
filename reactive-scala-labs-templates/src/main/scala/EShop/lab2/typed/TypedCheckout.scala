package EShop.lab2.typed

import akka.actor.Cancellable
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._
import scala.language.postfixOps

case class TypedCheckout() {

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  var cart: Option[ActorRef[TypedCommand]] = None

  def start: Behavior[TypedCommand] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case CheckoutStarted(c) =>
          println("[Init -> SelectDelivery] Checkout started")
          cart = Some(c)
          selectingDelivery(context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
        case _ =>
          println("[Init -> SelectDelivery] Checkout started")
          selectingDelivery(context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
    }
  )

  def selectingDelivery(timer: Cancellable): Behavior[TypedCommand] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case SelectDeliveryMethod(_) =>
            println("[SelectingDelivery -> SelectingPaymentMethod] Delivery method selected")
            selectingPaymentMethod(timer)
          case CancelCheckout =>
            println("[SelectingDelivery -> Cancelled] Checkout cancelled")
            cart.foreach(_ ! ConfirmCheckoutCancelled)
            cancelled
          case ExpireCheckout =>
            println("[SelectingDelivery -> Cancelled] Checkout expired")
            cart.foreach(_ ! ConfirmCheckoutCancelled)
            cancelled
          case _ => Behaviors.same
      }
    )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCommand] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case SelectPayment(_) =>
          println("[SelectingPaymentMethod -> ProcessingPayment] Payment selected")
          processingPayment(context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment))
        case CancelCheckout =>
          println("[SelectingPaymentMethod -> CancelCheckout] Checkout cancelled")
          cart.foreach(_ ! ConfirmCheckoutCancelled)
          cancelled
        case ExpireCheckout =>
          println("[SelectingPaymentMethod -> CancelCheckout] Checkout expired")
          cart.foreach(_ ! ConfirmCheckoutCancelled)
          cancelled
        case _ => Behaviors.same
    }
  )

  def processingPayment(timer: Cancellable): Behavior[TypedCommand] = Behaviors.receive { (context, msg) =>
    msg match {
      case ConfirmPaymentReceived =>
        println("[ProcessingPayment -> Closed] Payment confirmed")
        cart.foreach(_ ! ConfirmCheckoutClosed)
        closed
      case CancelCheckout =>
        println("[ProcessingPayment -> Cancelled] Checkout cancelled")
        cart.foreach(_ ! ConfirmCheckoutCancelled)
        cancelled
      case ExpirePayment =>
        println("[ProcessingPayment -> Cancelled] Payment expired")
        cart.foreach(_ ! ConfirmCheckoutCancelled)
        cancelled
      case _ => Behaviors.same
    }
  }

  def cancelled: Behavior[TypedCommand] = Behaviors.stopped

  def closed: Behavior[TypedCommand] = Behaviors.stopped

}
