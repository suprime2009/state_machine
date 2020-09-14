/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo.web

case class SaveEntityRequest(name: String)
case class ChangeStateRequest(state: String)
case class ChangeStateMessage(state: String, entityId: Int)
case class UpdateStateRequest(name: String, from: Seq[String], to: Seq[String])
