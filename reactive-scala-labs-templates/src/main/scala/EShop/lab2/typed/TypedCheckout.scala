package EShop.lab2.typed

import EShop.lab2.simple._
import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import scala.concurrent.duration._
import scala.language.postfixOps

object TypedCheckout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

}

class TypedCheckout {

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  def start: Behavior[Command] = Behaviors.receive(
    (context, _) => selectingDelivery(context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
  )

  def selectingDelivery(timer: Cancellable): Behavior[Command] =
    Behaviors.receive(
      (context: ActorContext[Command], msg) =>
        msg match {
          case SelectDeliveryMethod(_) => selectingPaymentMethod(timer)
          case CancelCheckout =>
            context.self ! ConfirmCheckoutCancelled
            cancelled
          case ExpireCheckout =>
            context.self ! ConfirmCheckoutCancelled
            cancelled
          case _ => Behaviors.same
      }
    )

  def selectingPaymentMethod(timer: Cancellable): Behavior[Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case SelectPayment(_) =>
          processingPayment(context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment))
        case CancelCheckout =>
          context.self ! ConfirmCheckoutCancelled
          cancelled
        case ExpireCheckout =>
          context.self ! ConfirmCheckoutCancelled
          cancelled
        case _ => Behaviors.same
    }
  )

  def processingPayment(timer: Cancellable): Behavior[Command] = Behaviors.receive { (context, msg) =>
    msg match {
      case ConfirmPaymentReceived =>
        context.self ! ConfirmCheckoutClosed
        closed
      case CancelCheckout =>
        context.self ! ConfirmCheckoutCancelled
        cancelled
      case ExpirePayment =>
        context.self ! ConfirmCheckoutCancelled
        cancelled
      case _ => Behaviors.same
    }
  }

  def cancelled: Behavior[Command] = Behaviors.stopped

  def closed: Behavior[Command] = Behaviors.stopped

}
