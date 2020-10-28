package myActorTest

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import akka.testkit.TestActorRef
import org.scalatest.wordspec.AnyWordSpecLike

class CounterSpec extends TestKit(ActorSystem("CounterSpec")) with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit =
    system.terminate

  "A Counter" must {

    "increment the value" in {
      import Counter._
      val counter = TestActorRef[Counter]
      counter ! Incr
      assert(counter.underlyingActor.count == 1)
    }

  }

}
