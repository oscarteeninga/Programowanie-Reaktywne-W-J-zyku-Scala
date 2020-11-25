package EShop.lab5

import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationInt}

object PaymentService {

  case object PaymentSucceeded // http status 200
  class PaymentClientError extends Exception // http statuses 400, 404
  class PaymentServerError extends Exception // http statuses 500, 408, 418

  def props(method: String, payment: ActorRef) = Props(new PaymentService(method, payment))

}

class PaymentService(method: String, payment: ActorRef) extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private val http = Http(context.system)
  private val URI  = getURI

  override def preStart(): Unit =
    http.singleRequest(HttpRequest(uri = URI)).pipeTo(self)

  override def receive: Receive = {
    case resp @ HttpResponse(StatusCodes.OK, _, _, _) =>
      payment ! PaymentSucceeded
    case resp @ HttpResponse(StatusCodes.RequestTimeout, _, _, _) =>
      throw new PaymentServerError
    case resp @ HttpResponse(StatusCodes.NotFound, _, _, _) =>
      throw new PaymentClientError
  }

  def shutdown(): Future[Terminated] = {
    Await.result(http.shutdownAllConnectionPools(),Duration.Inf)
    context.system.terminate()
  }

  private def getURI: String = method match {
    case "payu"   => "http://127.0.0.1:8080"
    case "paypal" => s"http://httpbin.org/status/408"
    case "visa"   => s"http://httpbin.org/status/200"
    case _        => s"http://httpbin.org/status/404"
  }
}
