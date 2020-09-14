/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo.model

case class Entity(id: Int, name: String, state: String)

case class ChangeStateResponse(entity_id: Int, from: String, to: String)

case class State(id: Int, name: String, from: Seq[String], to: Seq[String])
