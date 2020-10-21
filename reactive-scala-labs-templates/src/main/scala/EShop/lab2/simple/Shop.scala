package EShop.lab2.simple

import EShop.lab2._
import akka.actor.ActorSystem

object Shop extends App {
  val system = ActorSystem("sys")

  val cart = system.actorOf(CartActor.props, "cart")

  var checkout1 = system.actorOf(Checkout.props, "checkout1")
  cart ! AddItem("1")
  Thread.sleep(10)
  cart ! RemoveItem("1")
  Thread.sleep(10)
  cart ! AddItem("2")
  Thread.sleep(10)
  cart ! AddItem("3")
  Thread.sleep(10)
  cart ! AddItem("4")
  Thread.sleep(10)
  cart ! StartWithCheckout(checkout1)
  Thread.sleep(10)
  checkout1 ! SelectDeliveryMethod("DPD")
  Thread.sleep(10)
  checkout1 ! SelectPayment("BLIK")
  Thread.sleep(10)
  checkout1 ! ConfirmPaymentReceived
  Thread.sleep(1000)

  var checkout2 = system.actorOf(Checkout.props, "checkout2")
  cart ! AddItem(1232)
  Thread.sleep(10)
  cart ! StartWithCheckout(checkout2)
  Thread.sleep(10)
  checkout2 ! SelectDeliveryMethod("DPD")
  Thread.sleep(10)
  checkout2 ! SelectPayment("BLIK")
  Thread.sleep(10)
  checkout2 ! ConfirmPaymentReceived
}
