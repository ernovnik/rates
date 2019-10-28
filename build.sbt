name := "rates"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += Resolver.sonatypeRepo("releases")

lazy val app = (project in file("app")).
  settings(
    mainClass in assembly := Some("ru.ernovnik.rates.Entry")
  )

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.10"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.23"
libraryDependencies += "com.lihaoyi" %% "requests" % "0.2.0" // sbt
libraryDependencies += "io.circe" %% "circe-parser" % "0.12.3"
libraryDependencies += "io.circe" %% "circe-generic" % "0.12.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http-caching" % "10.1.10"
