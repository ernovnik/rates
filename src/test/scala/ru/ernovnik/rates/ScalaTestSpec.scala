package ru.ernovnik.rates

import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._


class ScalaTestSpec extends WordSpec with Matchers with ScalatestRouteTest {

  val route: Route =
    concat(
      get {
        path("convert") {
          parameters("from".as[String],"to".as[String], "number".as[Int]) { (from:String, to:String, number: Int) =>
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, RateService.calculate(from, to, number)))
          }
        }
      }
    )

  "The service" should {
    "convert from EUR to EUR" in {
      // tests:
      Get("/convert?from=EUR&to=EUR&number=100") ~> route ~> check {
        responseAs[String] shouldEqual "100"
      }
    }
    "convert from XYZ to XYZ" in {
      // tests:
      Get("/convert?from=XYZ&to=XZY&number=100") ~> route ~> check {
        responseAs[String] shouldEqual "error getting currencies from & to"
      }
    }
    "convert from EUR to XYZ" in {
      // tests:
      Get("/convert?from=EUR&to=XZY&number=100") ~> route ~> check {
        responseAs[String] shouldEqual "error getting currency to"
      }
    }
    "convert from XYZ to EUR" in {
      // tests:
      Get("/convert?from=XZY&to=EUR&number=100") ~> route ~> check {
        responseAs[String] shouldEqual "error getting currency from"
      }
    }
    "convert from USD to RUB" in {
      // tests:
      Get("/convert?from=USD&to=RUB&number=100") ~> route ~> check {
        responseAs[String] shouldNot contain("error")
      }
    }
    "convert from CAD to JPY" in {
      // tests:
      Get("/convert?from=CAD&to=JPY&number=12345") ~> route ~> check {
        responseAs[String] shouldNot contain("error")
      }
    }
  }


}
