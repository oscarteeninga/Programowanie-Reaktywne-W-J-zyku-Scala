package EShop.lab2

import EShop.lab2.TypedCartActor.{AddItem, StartCheckout}
import EShop.lab2.TypedCheckout.{CancelCheckout, ConfirmPaymentReceived, SelectDeliveryMethod, SelectPayment}
import akka.actor.typed.ActorSystem

object TypedShopApp extends App {

  val system    = ActorSystem(new TypedCartActor().start, "system")
  val cart      = system.systemActorOf(new TypedCartActor().start, "cart")
  val checkout1 = system.systemActorOf(new TypedCheckout(cart).start, "typedCheckout1")
  val checkout2 = system.systemActorOf(new TypedCheckout(cart).start, "typedCheckout2")

  cart ! AddItem(10)
  cart ! AddItem(10)
  cart ! AddItem(10)
  cart ! AddItem(10)
  cart ! StartCheckout
  checkout1 ! TypedCheckout.StartCheckout
  checkout1 ! CancelCheckout

  Thread.sleep(100)
  cart ! AddItem(20)
  cart ! StartCheckout
  checkout2 ! TypedCheckout.StartCheckout
  checkout2 ! SelectDeliveryMethod("DPD")
  checkout2 ! SelectPayment("BLIK")
  checkout2 ! ConfirmPaymentReceived

  Thread.sleep(100)
  system.terminate()
}
