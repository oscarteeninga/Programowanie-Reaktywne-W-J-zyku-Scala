package reactive5

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.Await
import scala.concurrent.duration._

class HTTPActor extends Actor
  with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  override def preStart() = {
    http.singleRequest(HttpRequest(uri = "http://localhost:8080/hello"))
      .pipeTo(self)
  }

  def receive = {
    case resp @ HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
            println("Got response, body: " + body.utf8String)
        resp.discardEntityBytes()
        shutdown()
      }
    case resp @ HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      shutdown()
      
  }

 def shutdown() = {
    Await.result(http.shutdownAllConnectionPools(),Duration.Inf)
      context.system.terminate()
 }
}


object Main {

  def main(args: Array[String]) {
 
    import system.dispatcher
    
    val system = ActorSystem("http-system")
    val httpActor = system.actorOf(Props[HTTPActor], "HTTP-Actor")
   
    Await.ready(system.whenTerminated, Duration.Inf) 
  }
}
