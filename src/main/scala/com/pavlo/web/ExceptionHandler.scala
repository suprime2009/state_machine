package com.pavlo.web

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import com.pavlo.model.{NotFoundError, ValidationError}
import spray.json.DefaultJsonProtocol.{jsonFormat1, _}
import spray.json.RootJsonFormat

final case class ErrorResponse(error: String)

object ErrorResponse {
  implicit val formatter: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse.apply)
}

object ExceptionHandlerDirective extends Directives with SprayJsonSupport {

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ve: ValidationError => completeError(StatusCodes.BadRequest, ve.getMessage)
    case nf: NotFoundError   => completeError(StatusCodes.NotFound, nf.getMessage)
    case _                   => completeError(StatusCodes.InternalServerError, "Server error!")
  }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handleNotFound {
      completeError(StatusCodes.NotFound, "Not found")
    }
    .result()

  def completeError(statusCode: StatusCode, msg: String): Route =
    complete((statusCode, ErrorResponse(msg)))
}
