/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo.web

import com.pavlo.model.{ChangeStateResponse, Entity, State}
import io.circe.Encoder
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

trait Codecs {

  implicit val changeStateDecode: RootJsonFormat[ChangeStateRequest] = jsonFormat1(ChangeStateRequest.apply)
  implicit val modifyStateDecode: RootJsonFormat[UpdateStateRequest] = jsonFormat3(UpdateStateRequest.apply)
  implicit val saveEntityDecode: RootJsonFormat[SaveEntityRequest] = jsonFormat1(SaveEntityRequest.apply)

  implicit val entityEncode: Encoder[Entity] =
    Encoder.forProduct3("id", "name", "state")(p => (p.id, p.name, p.state))

  implicit val stateEncode: Encoder[State] =
    Encoder.forProduct4("id", "name", "from", "to")(p => (p.id, p.name, p.from, p.to))

  implicit val changeStateEncode: Encoder[ChangeStateResponse] =
    Encoder.forProduct3("entityId", "from", "to")(p => (p.entity_id, p.from, p.to))
}