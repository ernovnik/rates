package ru.ernovnik.rates

import io.circe.{Decoder, Json}
import io.circe.parser.parse
import javax.net.ssl.{HttpsURLConnection, SSLContext}

object RateService {
  val url = "https://api.ratesapi.io/api/latest"
  val sslContext = SSLContext.getInstance("SSL")
  sslContext.init(null, Array(TrustAll), new java.security.SecureRandom())
  HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory)
  HttpsURLConnection.setDefaultHostnameVerifier(VerifiesAllHostNames)
  val baseCurrency = "EUR"
  def calculate(from: String, to: String, number: BigDecimal): String ={
    def findRate(json: Json, rate: String) : Decoder.Result[BigDecimal] =
      json.hcursor.downField("rates").downField(rate.toUpperCase).as[BigDecimal]
    parse(requests.get(url).text()) match {
      case Left(failure) => s"${failure.message}"
      case Right(json) => {
        (findRate(json,from.toUpperCase),
          findRate(json,to.toUpperCase)) match {
          case (Right(f1), Right(f2)) => s"${f2 / f1 * number}"
          case (Right(_), Left(_)) if from == baseCurrency => "error getting currency to"
          case (Left(_), Right(_)) if to == baseCurrency => "error getting currency from"
          case (Right(f1), Left(_)) if to == baseCurrency => s"${number/f1}"
          case (Left(_), Right(f2)) if from == baseCurrency => s"${number*f2}"
          case (Right(_), Left(f2)) => s"${f2}"
          case (Left(_), Right(_)) => "error getting currency from"
          case (Right(_), Left(_)) => "error getting currency to"
          case (Left(_), Left(_)) if from == baseCurrency && to == baseCurrency => s"$number"
          case (Left(_), Left(_)) if from == baseCurrency => "error getting currency to"
          case (Left(_), Left(_)) if to == baseCurrency => "error getting currency from"
          case (Left(_), Left(_)) => "error getting currencies from & to"
        }
      }
    }
  }
}