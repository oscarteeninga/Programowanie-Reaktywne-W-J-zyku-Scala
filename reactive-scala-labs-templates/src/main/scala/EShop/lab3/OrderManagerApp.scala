package EShop.lab3

import EShop.lab3.OrderManager._
import akka.actor.{ActorRef, ActorSystem}

object OrderManagerApp extends App {
  val system       = ActorSystem("system")
  val orderManager = system.actorOf(OrderManager.props, "orderManager")

  orderManager ! AddItem("1")
  orderManager ! AddItem("2")

  Thread.sleep(100)
  orderManager ! Buy
  Thread.sleep(100)
  orderManager ! SelectDeliveryAndPaymentMethod("DPD", "BLIK")
  Thread.sleep(100)
  orderManager ! Pay
  Thread.sleep(100)
}
