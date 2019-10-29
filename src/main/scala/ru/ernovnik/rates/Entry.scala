package ru.ernovnik.rates



import java.security.cert.X509Certificate
import akka.http.scaladsl.server.directives.CachingDirectives._
import akka.actor.ActorSystem
import akka.dispatch.forkjoin.ForkJoinPool
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.stream.ActorMaterializer
import javax.net.ssl._
import scala.concurrent.duration._


import scala.concurrent.ExecutionContext
import scala.io.StdIn

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
  val url = "https://api.ratesapi.io/api/latest"
  val sslContext = SSLContext.getInstance("SSL")
  sslContext.init(null, Array(TrustAll), new java.security.SecureRandom())
  HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory)
  HttpsURLConnection.setDefaultHostnameVerifier(VerifiesAllHostNames)
  val baseCurrency = "EUR"

  val keyerFunction: PartialFunction[RequestContext, Uri] = {
    case r: RequestContext => r.request.uri
  }
  val defaultCachingSettings = CachingSettings(system)
  val lfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(25)
      .withMaxCapacity(50)
      .withTimeToLive(20.seconds)
      .withTimeToIdle(10.seconds)
  val cachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)
  val lfuCache: Cache[Uri, RouteResult] = LfuCache(cachingSettings)


  val innerRoute: Route =
    concat(
      get {
        path("convert") {
          parameters("from".as[String],"to".as[String], "number".as[Int]) { (from:String, to:String, number: Int) =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, RateService.calculate(from, to, number)))
          }
        }
      }
    )

  val route = cache(lfuCache, keyerFunction)(innerRoute)

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
