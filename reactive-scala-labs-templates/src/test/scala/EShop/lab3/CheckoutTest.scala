package EShop.lab3

import EShop.lab2.CartActor.{AddItem, ConfirmCheckoutClosed}
import EShop.lab2.{Cart, CartActor, Checkout}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  import Checkout._

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "Send close confirmation to cart" in {

    val confirmMsg = "confirmMsg"

    val cart = system.actorOf(Props(new CartActor() {
      override def inCheckout(cart: Cart): Receive = {
        sender ! confirmMsg
        super.inCheckout(cart)
      }
    }))
    cart ! AddItem(1)
    cart ! CartActor.StartCheckout

    val checkout = system.actorOf(Checkout.props(cart))

    checkout ! Checkout.StartCheckout
    checkout ! SelectDeliveryMethod("DPD")
    checkout.!(SelectPayment("BLIK"))(ActorRef.noSender)
    checkout ! ConfirmPaymentReceived

    expectMsg(confirmMsg)
  }
}
