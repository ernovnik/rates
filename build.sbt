name := "rates"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += Resolver.sonatypeRepo("releases")

lazy val app = (project in file("app")).
  settings(
    mainClass in assembly := Some("ru.ernovnik.rates.Entry")
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10",
  "com.typesafe.akka" %% "akka-stream" % "2.5.23",
  "com.lihaoyi" %% "requests" % "0.2.0",
  "io.circe" %% "circe-parser" % "0.12.3",
  "io.circe" %% "circe-generic" % "0.12.3",
  "com.typesafe.akka" %% "akka-http-caching" % "10.1.10",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.10",
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

