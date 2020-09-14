package com.pavlo.db

import com.pavlo.model._
import com.pavlo.web.UpdateStateRequest
import slick.basic._
import slick.jdbc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

final class Repository(val dbConfig: DatabaseConfig[JdbcProfile]) {

  import dbConfig.profile.api._

  final class Entities(tag: Tag) extends Table[DbEntity](tag, "entities") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name", O.Unique)

    def stateId = column[Int]("stateId")

    def stateFk =
      foreignKey("stateFK", stateId, statesTable)(
        _.id,
        onDelete = ForeignKeyAction.Restrict,
        onUpdate = ForeignKeyAction.NoAction
      )

    def * = (id, name, stateId).shaped <> (DbEntity.tupled, DbEntity.unapply)
  }

  final class States(tag: Tag) extends Table[DbState](tag, "states") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name", O.Unique)

    def * = (id, name).shaped <> (DbState.tupled, DbState.unapply)
  }

  final class Transitions(tag: Tag) extends Table[DbTransition](tag, "transitions") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def fromState = column[Int]("fromState")

    def toState = column[Int]("toState")

    def fromStateFk =
      foreignKey("fromStateFk", fromState, statesTable)(
        _.id,
        onDelete = ForeignKeyAction.Cascade,
        onUpdate = ForeignKeyAction.Cascade
      )

    def toStateFk =
      foreignKey("toStateFK", toState, statesTable)(
        _.id,
        onDelete = ForeignKeyAction.Cascade,
        onUpdate = ForeignKeyAction.Cascade
      )

    def * = (id, fromState, toState).shaped <> (DbTransition.tupled, DbTransition.unapply)
  }

  final class OperationsTable(tag: Tag) extends Table[DbOperation](tag, "operations") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def entityId = column[Int]("entityId")

    def from = column[String]("fromState")

    def to = column[String]("toState")

    def fromStateFk =
      foreignKey("entityLogFK", entityId, entitiesTable)(
        _.id,
        onDelete = ForeignKeyAction.Cascade,
        onUpdate = ForeignKeyAction.Cascade
      )

    def * = (id, entityId, from, to).shaped <> (DbOperation.tupled, DbOperation.unapply)
  }

  val entitiesTable    = TableQuery[Entities]
  val statesTable      = TableQuery[States]
  val transitionsTable = TableQuery[Transitions]
  val operationsTable  = TableQuery[OperationsTable]

  val insertDBEntityQuery = entitiesTable returning entitiesTable.map(_.id) into (
        (
            entity,
            id
        ) => entity.copy(id = id)
    )
  val insertDBStateQuery = statesTable returning statesTable.map(_.id) into (
        (
            state,
            id
        ) => state.copy(id = id)
    )
  val insertDBTransitionsQuery = transitionsTable returning transitionsTable.map(_.id) into (
        (
            trans,
            id
        ) => trans.copy(id = id)
    )
  val insertDBOperationQuery = operationsTable returning operationsTable.map(_.id) into (
        (
            logs,
            id
        ) => logs.copy(transitionId = id)
    )

  def close(): Unit = dbConfig.db.close

  def loadStateByNameAction(name: String): Future[Option[DbState]] =
    dbConfig.db.run(loadStateByName(name))

  def loadStateByName(name: String): DBIO[Option[DbState]] =
    statesTable.filter(_.name === name).result.map(_.headOption)

  def loadState(id: Int): DBIO[Option[DbState]] =
    statesTable.filter(_.id === id).result.headOption

  def getTransition(fromAndToStates: (DbState, DbState)) =
    transitionsTable
      .filter(t => t.fromState === fromAndToStates._1.id && t.toState === fromAndToStates._2.id)
      .result
      .headOption

  def loadStates: Future[Seq[State]] = {
    val query = for {
      toSeq <- statesTable
        .joinLeft(transitionsTable)
        .on(_.id === _.fromState)
        .joinLeft(statesTable)
        .on(_._2.map(_.toState) === _.id)
        .result

      fromSeq <- statesTable
        .joinLeft(transitionsTable)
        .on(_.id === _.toState)
        .joinLeft(statesTable)
        .on(_._2.map(_.fromState) === _.id)
        .result

      fromStates = fromSeq.groupBy(_._1._1).mapValues(_.flatMap(_._2.map(_.name)))
      toState    = toSeq.groupBy(_._1._1).mapValues(_.flatMap(_._2.map(_.name)))

      states = (fromStates.keySet ++ toState.keySet)
        .map(
          k =>
            State(k.id, k.name, fromStates.getOrElse(k, Seq.empty), toState.getOrElse(k, Seq.empty))
        )
    } yield states.toSeq
    dbConfig.db.run(query)
  }

  def saveOrUpdateState(request: UpdateStateRequest): Future[State] = {
    val interaction = for {
      persistedState <- statesTable.filter(_.name === request.name.bind).result.headOption flatMap {
        case None          => insertDBStateQuery += DbState(0, request.name)
        case Some(dbState) => DBIO.successful(dbState)
      }

      _ <- transitionsTable
        .filter(t => t.toState === persistedState.id.bind || t.fromState === persistedState.id.bind)
        .delete

      fromStates <- DBIO.sequence(
        request.from
          .map(stateName => saveTransition(stateName, id => DbTransition(0, id, persistedState.id)))
      )
      toStates <- DBIO.sequence(
        request.to
          .map(stateName => saveTransition(stateName, id => DbTransition(0, persistedState.id, id)))
      )
    } yield State(persistedState.id, persistedState.name, fromStates, toStates)
    dbConfig.db.run(interaction.withTransactionIsolation(TransactionIsolation.RepeatableRead))
  }

  private def saveTransition(name: String, dbTransition: Int => DbTransition) =
    for {
      dbState <- statesTable.filter(_.name === name.bind).result.headOption flatMap {
        case None        => DBIO.failed(StateNotFound(name))
        case Some(state) => DBIO.successful(state)
      }
      _ <- insertDBTransitionsQuery += dbTransition(dbState.id)
    } yield name

  def loadEntities = {
    val query =
      entitiesTable.join(statesTable).on(_.stateId === _.id).sortBy(_._1.name)
    query.result.map(a => a.map(e => Entity(e._1.id, e._1.name, e._2.name)))
  }

  def loadEntity(entityId: Int): DBIO[Option[DbEntity]] =
    entitiesTable.filter(_.id === entityId).result.map(_.headOption)

  def loadEntityByNameAction(name: String): Future[Option[DbEntity]] = {
    val action = entitiesTable.filter(_.name === name.bind)
    dbConfig.db.run(action.result).map(_.headOption)
  }

  def saveEntity(data: DbEntity): Future[DbEntity] = {
    val interaction = for {
      persisted <- insertDBEntityQuery += data
    } yield persisted
    dbConfig.db.run(interaction.transactionally)
  }

  def updateState(entity: DbEntity, toState: DbState) =
    entitiesTable.filter(_.id === entity.id).update(entity.copy(stateId = toState.id))

  def insertLog(log: DbOperation) =
    insertDBOperationQuery += log
}
