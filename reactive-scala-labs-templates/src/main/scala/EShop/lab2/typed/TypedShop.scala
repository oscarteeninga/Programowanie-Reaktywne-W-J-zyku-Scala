package EShop.lab2.typed

import EShop.lab2.Cart
import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.FiniteDuration

object TypedShop extends App {
  val cart      = new TypedCartActor()
  val checkout1 = new TypedCheckout

  Behaviors

}
