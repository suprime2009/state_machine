package com.pavlo.service

import cats.data.EitherT
import cats.implicits._
import com.pavlo.db.{ DbEntity, DbOperation, DbState, Repository }
import com.pavlo.model._
import com.pavlo.web.{ ChangeStateMessage, SaveEntityRequest, UpdateStateRequest }
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Service(repository: Repository)(val dbConfig: DatabaseConfig[JdbcProfile]) {

  import dbConfig.profile.api._

  def selectAllStates: Future[Seq[State]] = repository.loadStates

  def createOrUpdateState(request: UpdateStateRequest): Future[State] = {
    for {
      _         <- EitherT.fromEither[Future](checkUpdateStateRequest(request.from)(request.name))
      _         <- EitherT.fromEither[Future](checkUpdateStateRequest(request.to)(request.name))
      persisted <- EitherT.right[ElementError](repository.saveOrUpdateState(request))
    } yield persisted
  }.valueOrF(Future.failed(_))

  def selectAllEntities: Future[Seq[Entity]] = dbConfig.db.run {
    repository.loadEntities
  }

  def saveEntity(request: SaveEntityRequest): Future[Entity] = {
    for {
      _ <- EitherT.right[ElementError](repository.loadEntityByNameAction(request.name).reject {
        case dbEntity if dbEntity.isDefined => EntityAlreadyExists(request.name)
      })
      dbState <- EitherT.fromOptionF(
        repository.loadStateByNameAction("init"),
        StateNotFound("init")
      )
      dbEntity <- EitherT.right[ElementError](
        repository.saveEntity(DbEntity(0, request.name, dbState.id))
      )
    } yield Entity(dbEntity.id, dbEntity.name, dbState.name)
  }.valueOrF(Future.failed(_))

  def moveEntity(request: ChangeStateMessage): Future[ChangeStateResponse] = {
    val actions = for {
      dbEntity <- repository.loadEntity(request.entityId).map {
        _.getOrElse(throw EntityNotFound)
      }
      stateTo <- repository.loadStateByName(request.state).map {
        _.getOrElse(throw StateNotFound(request.state))
      }
      stateFrom <- repository.loadState(dbEntity.stateId).map {
        _.getOrElse(throw StateNotFound(request.state))
      }

      _ <- repository.getTransition((stateFrom, stateTo)).map {
        _.getOrElse(throw StateChangeNotAllowed(stateFrom.name, stateTo.name))
      }
      _ <- repository.updateState(dbEntity, stateTo)
      _ <- repository.insertLog(DbOperation(0, dbEntity.id, stateFrom.name, stateTo.name))
      response = ChangeStateResponse(dbEntity.id, stateFrom.name, stateTo.name)
    } yield response
    dbConfig.db.run(actions.transactionally)
  }

  private def checkUpdateStateRequest(states: Seq[String])(stateName: String) =
    Either.cond(!states.contains(stateName), stateName, StateInvalidRequest)
}
