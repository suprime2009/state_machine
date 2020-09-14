/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo.model

abstract class ElementError(msg: String, cause: Exception)
  extends Exception(msg, cause, false, false)

abstract class ValidationError(msg: String) extends ElementError(msg, null)

abstract class NotFoundError(msg: String) extends ElementError(msg, null)

case object EntityNotFound extends NotFoundError("Entity not found in DB.")

case class StateNotFound(name: String) extends NotFoundError(s"State $name not found.")

case class StateChangeNotAllowed(from: String, to: String)
  extends ValidationError(s"State change from $from to state $to not supported.")

case class EntityAlreadyExists(name: String)
  extends ValidationError(s"Entity with name $name already exists.")

case object StateInvalidRequest
  extends ValidationError("You can't create state with transition to itself.")
