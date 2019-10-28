import java.security.cert.X509Certificate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.dispatch.forkjoin.ForkJoinPool
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import javax.net.ssl.{HostnameVerifier, HttpsURLConnection, SSLContext, SSLSession, X509TrustManager}

import scala.concurrent.ExecutionContext
import scala.io.{Source, StdIn}

object TrustAll extends X509TrustManager {
  val getAcceptedIssuers = null

  def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) = {}

  def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) = {}
}

object VerifiesAllHostNames extends HostnameVerifier {
  def verify(s: String, sslSession: SSLSession) = true
}


object Entry extends App{

  val pool = new ForkJoinPool(50)
  implicit val ec = ExecutionContext.fromExecutor(pool)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val route: Route =
    concat(
      get {
        path("convert")
        parameterMap { params =>

          def paramString(param: (String, String)): String = s"""${param._1} = '${param._2}'"""

          complete(s"The parameters are ${params.mkString(", ")}")

//          complete(s"The parameters are ${params.map(paramString).mkString(", ")}")
        }
      }


//        {
//          parameterMap
//          val url = "https://api.ratesapi.io/api/latest"
//          val sslContext = SSLContext.getInstance("SSL")
//          sslContext.init(null, Array(TrustAll), new java.security.SecureRandom())
//          HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory)
//          HttpsURLConnection.setDefaultHostnameVerifier(VerifiesAllHostNames)
//
//          val r = requests.get(url)
//
//
//          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"${r.text}"))
//
////          onSuccess(maybeItem) {
////            case Some(item) => complete(item)
////            case None       => complete(StatusCodes.NotFound)
////          }
//        }
//      }
,
      post {
        path("create-order") {
          entity(as[String]) { order =>
//            val saved: Future[Done] = saveOrder(order)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>telegram is cool</h1>"))
//            onComplete(saved) { done =>
//              complete("order created")
//            }
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
