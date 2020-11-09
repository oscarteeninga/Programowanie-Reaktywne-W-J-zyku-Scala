package reactive4.persistence

import akka.actor.{actorRef2Scala, Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.persistence._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

// states
object PersistentToggle {
  sealed trait State
  case object Happy extends State
  case object Sad   extends State

  case class MoodChanged(state: State)
}

class PersistentToggle extends PersistentActor {

  import PersistentToggle._

  override def persistenceId = "persistent-toggle-id-1"

  def updateState(event: MoodChanged): Unit =
    context.become(event.state match {
      case Happy => happy
      case Sad   => sad
    })

  def happy: Receive = LoggingReceive {
    case "How are you?" =>
      persist(MoodChanged(Sad)) { event =>
        updateState(event)
        sender ! "happy"
      }
    case "Done" =>
      sender ! "Done"
      context.stop(self)
  }

  def sad: Receive = LoggingReceive {
    case "How are you?" =>
      persist(MoodChanged(Happy)) { event =>
        updateState(event)
        sender ! "sad"
      }
    case "Done" =>
      sender ! "Done"
      context.stop(self)
  }

  def receiveCommand: Receive = happy

  val receiveRecover: Receive = {
    case evt: MoodChanged => updateState(evt)
  }
}

class ToggleMain extends Actor {

  val toggle: ActorRef = context.actorOf(Props[PersistentToggle], "toggle")

  def receive: Receive = LoggingReceive {
    case "Init" =>
      toggle ! "How are you?"
      toggle ! "How are you?"
      toggle ! "How are you?"
      toggle ! "Done"
    case "Done" =>
      println("Terminating")
      context.system.terminate()
    case msg: String =>
      println(s" received: $msg")
  }
}

object PersistentToggleApp extends App {
  val system    = ActorSystem("Reactive4")
  val mainActor = system.actorOf(Props[ToggleMain], "mainActor")

  mainActor ! "Init"

  Await.result(system.whenTerminated, Duration.Inf)
}
