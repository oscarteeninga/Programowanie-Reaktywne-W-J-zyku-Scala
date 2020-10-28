package EShop.lab2

import EShop.lab2.CartActor.{AddItem, ConfirmCheckoutCancelled, ConfirmCheckoutClosed, RemoveItem, StartCheckout}
import EShop.lab2.Checkout.{CancelCheckout, CheckOutClosed, ConfirmPaymentReceived, SelectDeliveryMethod, SelectPayment}
import akka.actor.ActorSystem

object ShopApp extends App {
  val system = ActorSystem("sys")

  val cart      = system.actorOf(CartActor.props, "cart")
  var checkout1 = system.actorOf(Checkout.props(cart), "checkout1")
  var checkout2 = system.actorOf(Checkout.props(cart), "checkout2")

  cart ! AddItem("1")
  cart ! RemoveItem("1")
  cart ! AddItem("2")
  cart ! AddItem("3")
  cart ! AddItem("4")
  cart ! StartCheckout
  checkout1 ! Checkout.StartCheckout
  checkout1 ! SelectDeliveryMethod("DPD")
  checkout1 ! SelectPayment("BLIK")
  checkout1 ! CancelCheckout
  checkout1 ! CheckOutClosed
  cart ! ConfirmCheckoutCancelled

  Thread.sleep(1000)
  cart ! AddItem(1232)
  cart ! StartCheckout
  checkout2 ! Checkout.StartCheckout
  checkout2 ! SelectDeliveryMethod("DPD")
  checkout2 ! SelectPayment("BLIK")
  checkout2 ! ConfirmPaymentReceived
  checkout2 ! CheckOutClosed
  cart ! ConfirmCheckoutClosed

  system.stop(cart)
  system.terminate()
}
