package EShop.lab2.simple

import akka.actor.ActorRef

trait Event
case class CheckoutStarted(actorRef: ActorRef) extends Event
case object ConfirmCheckoutCancelled           extends Event
case object ConfirmCheckoutClosed              extends Event
case object ConfirmPaymentReceived             extends Event
case class PaymentStarted(payment: ActorRef)   extends Event
