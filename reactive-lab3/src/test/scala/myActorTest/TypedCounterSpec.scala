package myActorTest

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TypedCounterSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  "A TypedCounter" must {

    "increment the value" in {
      import TypedCounter._
      val counter = testKit.spawn(TypedCounter(0))
      val probe   = testKit.createTestProbe[TypedCounterMain.Command]()
      counter ! Incr
      counter ! Get(probe.ref)
      probe.expectMessage(TypedCounterMain.Count(1))
    }

  }

}
