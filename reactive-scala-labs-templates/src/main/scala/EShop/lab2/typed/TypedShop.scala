package EShop.lab2.typed

import akka.actor.typed.{ActorSystem, Behavior}

object TypedShop extends App {

  val cart      = ActorSystem(TypedCartActor().start, "typedCart")
  val checkout1 = ActorSystem(TypedCheckout().start, "typedCheckout1")
  val checkout2 = ActorSystem(TypedCheckout().start, "typedCheckout2")

  cart ! AddItem(10)
  cart ! AddItem(10)
  cart ! AddItem(10)
  cart ! AddItem(10)
  cart ! StartCheckout
  checkout1 ! CheckoutStarted(cart)
  checkout1 ! CancelCheckout
  Thread.sleep(100)
  cart ! AddItem(20)
  cart ! StartCheckout
  checkout2 ! CheckoutStarted(cart)
  checkout2 ! SelectDeliveryMethod("DPD")
  checkout2 ! SelectPayment("BLIK")
  checkout2 ! ConfirmPaymentReceived
}
