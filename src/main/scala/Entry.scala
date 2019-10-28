import java.security.cert.X509Certificate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.dispatch.forkjoin.ForkJoinPool
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import javax.net.ssl.{HostnameVerifier, HttpsURLConnection, SSLContext, SSLSession, X509TrustManager}
import io.circe._, io.circe.parser._
import scala.concurrent.ExecutionContext
import scala.io.{Source, StdIn}
import io.circe.generic.JsonCodec, io.circe.syntax._

object TrustAll extends X509TrustManager {
  val getAcceptedIssuers = null

  def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) = {}

  def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) = {}
}

object VerifiesAllHostNames extends HostnameVerifier {
  def verify(s: String, sslSession: SSLSession) = true
}

case class Rates(base: String, rates: Map[String,BigDecimal], date: String)

object Entry extends App{

  val pool = new ForkJoinPool(50)
  implicit val ec = ExecutionContext.fromExecutor(pool)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val route: Route =
    concat(
      get {
        path("convert") {
          parameterMap { params => {
            def paramString(param: (String, String, BigDecimal)): String = s"""${param._1} = '${param._2}'"""
            val url = "https://api.ratesapi.io/api/latest"
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, Array(TrustAll), new java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(VerifiesAllHostNames)
            val r = requests.get(url)
            parse(r.text()) match {
              case Left(failure) => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"${failure.message}"))
              case Right(json) => {

                val from = params("from").toString
                val to = params("to").toString
                val number = BigDecimal(params("number").toString)

                (json.hcursor.downField("rates").downField(from).as[BigDecimal],
                  json.hcursor.downField("rates").downField(to).as[BigDecimal]) match {
                  case (Right(f1), Right(f2)) => {
                    val result = f2 / f1 * number
                    complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"$result"))
                  }
                  case (Right(f1),Left(f2))=>complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"${f2}"))
                  case (Left(f1),Right(f2))=>complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"${f1}"))
                  case (Left(f1),Left(f2))=>complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"${f1} ${f2}"))
                }
              }
            }
          }
          }
        }
      }
    )

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
