package io.digitalmagic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ddata._
import akka.cluster.ddata.Replicator._

import scala.concurrent.duration._
import scala.language.postfixOps

object ReplicatedCache {
  sealed trait Response
  final case object Updated extends Response
  final case class Entry(key: String, value: String) extends Response
  final case class NoSuchKey(key: String) extends Response
  final case class ReadFailure(key: String) extends Response
  final case class WriteFailure(key: String, msg: String) extends Response

  final case class Set(key: String, value: String)
  final case class Delete(key: String)
  final case class Read(key: String)

  final case class Pending(replyTo: ActorRef, key: String)

  def props: Props = Props[ReplicatedCache]
}

class ReplicatedCache extends Actor with ActorLogging {
  import ReplicatedCache._

  private val replicator = DistributedData(context.system).replicator
  implicit val node: Cluster = Cluster(context.system)
  private val dataKey = LWWMapKey[String, String]("cache")

  def receive: Receive = {
    case Read(key) =>
      log.debug(s"got read($key)")
      replicator ! Get(dataKey, Replicator.ReadMajority(3 seconds), Some(Pending(sender(), key)))

    case Set(key, value) =>
      log.debug(s"got set($key, $value)")
      replicator ! Update(dataKey, LWWMap.empty[String, String], Replicator.WriteMajority(3 seconds), Some(Pending(sender(), key)))(_ + (key -> value))

    case Delete(key) =>
      log.debug(s"got delete($key)")
      replicator ! Update(dataKey, LWWMap.empty[String, String], Replicator.WriteMajority(3 seconds), Some(Pending(sender(), key)))(_ - key)

    case UpdateSuccess(`dataKey`, Some(req: Pending)) =>
      log.debug(s"update successful for $req")
      req.replyTo ! Updated

    case UpdateTimeout(`dataKey`, Some(req: Pending)) =>
      log.debug(s"update timed out for $req")
      req.replyTo ! WriteFailure(req.key, "operation timed out")

    case ModifyFailure(`dataKey`, msg, error, Some(req: Pending)) =>
      log.error(error, s"failed to update ${req.key} due to: $msg")
      req.replyTo ! WriteFailure(req.key, msg)

    case r@GetSuccess(`dataKey`, Some(req: Pending)) =>
      log.debug(s"got reply for $req - ${r.dataValue.asInstanceOf[LWWMap[String, String]].entries.mkString(", ")}")
      r.get(dataKey).get(req.key) match {
        case Some(value: String) =>
          req.replyTo ! Entry(req.key, value)
        case None =>
          req.replyTo ! NoSuchKey(req.key)
      }

    case GetFailure(`dataKey`, Some(req: Pending)) ⇒
      log.debug(s"query for $req timed out")
      req.replyTo ! ReadFailure(req.key)

    case NotFound(key, Some(req: Pending)) =>
      log.debug(s"query for $req failed - cache is not initialized yet")
      req.replyTo ! NoSuchKey(req.key)

    case msg =>
      log.error(s"got unexpected $msg")
    // case StoreFailure(`dataKey`, Some(req: Pending)) =>
  }
}
