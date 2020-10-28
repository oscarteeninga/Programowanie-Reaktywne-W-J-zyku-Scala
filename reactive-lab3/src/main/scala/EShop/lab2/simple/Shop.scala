package EShop.lab2.simple

import akka.actor.ActorSystem

object Shop extends App {
  val system = ActorSystem("sys")

  val cart = system.actorOf(CartActor.props, "cart")

  var checkout1 = system.actorOf(Checkout.props, "checkout1")
  cart ! AddItem("1")
  cart ! RemoveItem("1")
  cart ! AddItem("2")
  cart ! AddItem("3")
  cart ! AddItem("4")
  Thread.sleep(10)
  cart ! StartCheckout
  checkout1 ! CheckoutStarted(cart)
  Thread.sleep(10)
  checkout1 ! SelectDeliveryMethod("DPD")
  checkout1 ! SelectPayment("BLIK")
  checkout1 ! CancelCheckout
  Thread.sleep(2000)

  var checkout2 = system.actorOf(Checkout.props, "checkout2")
  cart ! AddItem(1232)
  Thread.sleep(10)
  cart ! StartCheckout
  checkout2 ! CheckoutStarted(cart)
  Thread.sleep(10)
  checkout2 ! SelectDeliveryMethod("DPD")
  checkout2 ! SelectPayment("BLIK")
  checkout2 ! ConfirmPaymentReceived
}
