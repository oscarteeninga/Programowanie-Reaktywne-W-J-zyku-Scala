package myActorTest

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TypedToggleSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  import TypedToggleActor._

  "A TypedToggle" must {

    "start in a happy mood" in {
      val toggle = testKit.spawn(TypedToggleActor())
      val probe  = testKit.createTestProbe[String]()

      toggle ! HowAreYou(probe.ref)
      probe.expectMessage("happy")
    }

    "change its mood" in {
      val toggle = testKit.spawn(TypedToggleActor())
      val probe  = testKit.createTestProbe[String]()

      for (i <- 1 to 5) {
        toggle ! HowAreYou(probe.ref)
        probe.expectMessage("happy")
        toggle ! HowAreYou(probe.ref)
        probe.expectMessage("sad")
      }
    }

    "finish when done" in {
      val toggle = testKit.spawn(TypedToggleActor())
      val probe  = testKit.createTestProbe[String]()

      toggle ! Done(probe.ref)
      probe.expectMessage("Done")
    }

  }

}
