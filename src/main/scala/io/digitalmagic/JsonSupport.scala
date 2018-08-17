package io.digitalmagic

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import io.digitalmagic.ReplicatedCache.Entry

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val fmtEntry: RootJsonFormat[Entry] = jsonFormat2(Entry)
}
