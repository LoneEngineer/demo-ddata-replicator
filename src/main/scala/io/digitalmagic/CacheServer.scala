package io.digitalmagic

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object CacheServer extends App with AppRoutes {

  implicit val system: ActorSystem = ActorSystem("ReplicatedAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val replica: ActorRef = system.actorOf(ReplicatedCache.props, "Cache")

  lazy val routes: Route = appRoutes

  Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
}
