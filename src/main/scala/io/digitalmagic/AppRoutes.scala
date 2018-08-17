package io.digitalmagic

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete

import scala.concurrent.duration._
import scala.language.postfixOps
import ReplicatedCache._

trait AppRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[AppRoutes])

  def replica: ActorRef

  implicit lazy val timeout: Timeout = 5 seconds

  lazy val appRoutes: Route =
    pathPrefix("cache") {
      concat(
        pathEnd {
          post {
            entity(as[Entry]) { entry =>
              val setValue = (replica ? Set(entry.key, entry.value)).mapTo[Response]
              onSuccess(setValue) {
                case Updated =>
                  complete(StatusCodes.Created)
                case WriteFailure(_, msg) =>
                  complete(StatusCodes.InternalServerError, msg)
              }
            }
          }
        },
        path(Segment) { key =>
          concat(
            get {
              val getValue = (replica ? Read(key)).mapTo[Response]
              onSuccess(getValue) {
                case e:Entry =>
                  complete(e)
                case NoSuchKey(_) =>
                  complete(StatusCodes.NotFound)
                case ReadFailure(_) =>
                  complete(StatusCodes.RequestTimeout)
              }
            },
            delete {
              val deleteValue = (replica ? Delete(key)).mapTo[Response]
              onSuccess(deleteValue) {
                case Updated =>
                  complete(StatusCodes.NoContent)
                case WriteFailure(_, msg) =>
                  complete(StatusCodes.RequestTimeout, msg)
              }
            }
          )
        }
      )
    }
}
