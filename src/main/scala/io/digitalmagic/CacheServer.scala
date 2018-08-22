package io.digitalmagic

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer

object CacheServer extends App with AppRoutes {

  implicit val system: ActorSystem = ActorSystem("replicator")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  val replica: ActorRef = system.actorOf(ReplicatedCache.props, "Cache")

  lazy val routes: Route = appRoutes

  Http().bindAndHandle(routes, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
}
