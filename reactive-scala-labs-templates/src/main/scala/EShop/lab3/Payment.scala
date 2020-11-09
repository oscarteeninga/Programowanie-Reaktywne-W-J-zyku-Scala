package EShop.lab3

import EShop.lab2.Checkout
import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  sealed trait Event
  case object PaymentConfirmed extends Event

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))
}

class Payment(
  method: String,
  orderManager: ActorRef,
  checkout: ActorRef
) extends Actor {

  private val log = Logging(context.system, this)

  override def receive: Receive = {
    case Payment.DoPayment =>
      log.debug("[Receive] Payment done")
      orderManager ! OrderManager.ConfirmPaymentReceived
      checkout ! Checkout.ConfirmPaymentReceived
      context.stop(self)
  }
}
