package EShop.lab3

import EShop.lab2.{Cart, CartActor}
import akka.actor.{ActorSystem, Cancellable, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class CartTest
  extends TestKit(ActorSystem("CartTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  import CartActor._

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    val cart = system.actorOf(CartActor.props)

    cart ! AddItem("Hamlet")
    cart ! GetItems
    expectMsg(Seq("Hamlet"))
  }

  it should "be empty after adding and removing the same item" in {
    val cart = system.actorOf(CartActor.props)

    cart ! AddItem("Hamlet")
    cart ! RemoveItem("Hamlet")
    cart ! GetItems
    expectMsg(Seq.empty)
  }

  it should "start checkout" in {
    val checkoutMsg = "start checkout"

    val cart = system.actorOf(Props(new CartActor {
      override def inCheckout(cart: Cart): Receive = {
        sender ! checkoutMsg
        super.inCheckout(cart)
      }
    }))

    cart ! AddItem("Hamlet")
    cart ! StartCheckout
    expectMsg(checkoutMsg)
  }
}
