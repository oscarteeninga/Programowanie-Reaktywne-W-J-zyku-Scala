package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import scala.language.postfixOps

import scala.concurrent.duration._

object TypedCheckout {

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
  case object CheckOutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef[Any]) extends Event
}

class TypedCheckout(
  cartActor: ActorRef[TypedCartActor.Command]
) {
  import TypedCheckout._

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  def start: Behavior[TypedCheckout.Command] = Behaviors.setup(
    context => {
      println("[Init -> SelectDelivery] Checkout started")
      selectingDelivery(context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
    }
  )

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case SelectDeliveryMethod(_) =>
            println("[SelectingDelivery -> SelectingPaymentMethod] Delivery method selected")
            selectingPaymentMethod(timer)
          case CancelCheckout =>
            timer.cancel()
            println("[SelectingDelivery -> Cancelled] Checkout cancelled")
            cancelled
          case ExpireCheckout =>
            println("[SelectingDelivery -> Cancelled] Checkout expired")
            cancelled
          case _ => Behaviors.same
      }
    )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case SelectPayment(_) =>
          timer.cancel()
          println("[SelectingPaymentMethod -> ProcessingPayment] Payment selected")
          processingPayment(context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment))
        case CancelCheckout =>
          timer.cancel()
          println("[SelectingPaymentMethod -> CancelCheckout] Checkout cancelled")
          cancelled
        case ExpireCheckout =>
          println("[SelectingPaymentMethod -> CancelCheckout] Checkout expired")
          cancelled
        case _ => Behaviors.same
    }
  )

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive { (context, msg) =>
    msg match {
      case ConfirmPaymentReceived =>
        timer.cancel()
        println("[ProcessingPayment -> Closed] Payment confirmed")
        closed
      case CancelCheckout =>
        timer.cancel()
        println("[ProcessingPayment -> Cancelled] Checkout cancelled")
        cancelled
      case ExpirePayment =>
        println("[ProcessingPayment -> Cancelled] Payment expired")
        cancelled
      case _ => Behaviors.same
    }
  }

  def cancelled: Behavior[TypedCheckout.Command] = {
    println("[Cancelled] Checkout cancelled")
    Behaviors.stopped
  }

  def closed: Behavior[TypedCheckout.Command] = {
    println("[Closed] Checkout closed")
    Behaviors.stopped
  }
}
