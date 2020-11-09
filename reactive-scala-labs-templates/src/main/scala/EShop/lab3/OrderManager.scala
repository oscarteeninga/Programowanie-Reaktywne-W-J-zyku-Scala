package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import akka.actor.{Actor, ActorRef, Props}
import akka.event.{Logging, LoggingReceive}

object OrderManager {
  sealed trait State
  case object Uninitialized extends State
  case object Open          extends State
  case object InCheckout    extends State
  case object InPayment     extends State
  case object Finished      extends State

  sealed trait Command
  case class AddItem(id: String)                                               extends Command
  case class RemoveItem(id: String)                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy                                                              extends Command
  case object Pay                                                              extends Command
  case class ConfirmCheckoutStarted(checkoutRef: ActorRef)                     extends Command
  case class ConfirmPaymentStarted(paymentRef: ActorRef)                       extends Command
  case object ConfirmPaymentReceived                                           extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK

  def props = Props(new OrderManager())
}

class OrderManager extends Actor {

  private val log = Logging(context.system, this)

  override def receive: Receive = uninitialized

  def uninitialized: Receive =
    open(context.actorOf(CartActor.props))

  def open(cartActor: ActorRef): Receive = {
    case AddItem(id) =>
      sender ! OrderManager.Done
      cartActor ! CartActor.AddItem(id)
    case RemoveItem(id) =>
      cartActor ! CartActor.RemoveItem(id)
    case Buy =>
      cartActor ! CartActor.StartCheckout
      context become inCheckout(cartActor, sender)
    case msg: Any =>
      log.warning("[open] Unexpected message " + msg)
  }

  def inCheckout(cartActorRef: ActorRef, senderRef: ActorRef): Receive = {
    case ConfirmCheckoutStarted(checkoutRef) =>
      senderRef ! OrderManager.Done
      context become inCheckout(checkoutRef)
    case msg: Any =>
      log.warning("[inCheckout] Unexpected message " + msg)
  }

  def inCheckout(checkoutActorRef: ActorRef): Receive = {
    case SelectDeliveryAndPaymentMethod(delivery, payment) =>
      checkoutActorRef ! Checkout.SelectDeliveryMethod(delivery)
      checkoutActorRef ! Checkout.SelectPayment(payment)
      context become inPayment(sender)
    case msg: Any =>
      log.warning("[inCheckout] Unexpected message " + msg)
  }

  def inPayment(senderRef: ActorRef): Receive = {
    case ConfirmPaymentStarted(paymentRef) =>
      senderRef ! OrderManager.Done
      context become inPayment(paymentRef, senderRef)
    case msg: Any =>
      log.warning("[inPayment] Unexpected message " + msg)
  }

  def inPayment(paymentActorRef: ActorRef, senderRef: ActorRef): Receive = {
    case Pay =>
      paymentActorRef ! Payment.DoPayment
      log.debug("[InPayment] Do payment")
      context become inPayment(paymentActorRef, sender)
    case ConfirmPaymentReceived =>
      senderRef ! OrderManager.Done
      log.debug("[inPayment -> finished] Confirmed payment received")
      context become finished
    case msg: Any =>
      log.warning("[inPayment] Unexpected message " + msg)

  }

  def finished: Receive = _ => {
    sender ! "order manager finished job"
    log.debug("[inPayment] Order manager finished job")
  }
}
