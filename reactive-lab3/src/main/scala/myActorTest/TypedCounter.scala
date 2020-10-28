package myActorTest

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.duration._
import scala.concurrent.Await

object TypedCounter {
  trait Command
  case object Incr                                            extends Command
  case class Get(replyTo: ActorRef[TypedCounterMain.Command]) extends Command

  def apply(count: Int): Behavior[Command] = Behaviors.receiveMessage {
    case Incr =>
      println("Thread name: " + Thread.currentThread.getName + ".")
      apply(count + 1)
    case Get(replyTo) =>
      replyTo ! TypedCounterMain.Count(count) // "!" operator is pronounced "tell" in Akka
      Behaviors.same
  }
}

object TypedCounterMain {
  trait Command
  case object Init             extends Command
  case class Count(count: Int) extends Command

  def apply(): Behavior[Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case Init =>
            val counter = context.spawn(TypedCounter(0), "counter")
            counter ! TypedCounter.Incr
            counter ! TypedCounter.Incr
            counter ! TypedCounter.Incr
            counter ! TypedCounter.Get(context.self)
            Behaviors.same
          case Count(count) =>
            println(s"count received: $count")
            println(Thread.currentThread.getName + ".")
            context.system.terminate
            Behaviors.stopped
      }
    )
}

object TypedApplicationCounter extends App {
  val system = ActorSystem(TypedCounterMain(), "mainActor")

  system ! TypedCounterMain.Init

  Await.result(system.whenTerminated, Duration.Inf)
}
