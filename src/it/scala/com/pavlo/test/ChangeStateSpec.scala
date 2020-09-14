package com.pavlo.test

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, _}
import akka.util.ByteString
import com.pavlo.BaseUseCaseSpec
import com.pavlo.db.DbEntity
import com.pavlo.web.{ChangeStateRequest, Codecs, UpdateStateRequest}
import io.circe.syntax._

import scala.collection.immutable.Seq

class ChangeStateSpec extends BaseUseCaseSpec with Codecs {
  private final val http = Http()

  override protected def beforeEach(): Unit = {
    flyway.clean()
    val _ = flyway.migrate()
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    flyway.clean()
    super.afterEach()
  }

  "Change entity state" when {
    val toUnknown = ChangeStateRequest("unknown")
    "state not found" in {
      for {
        dbEntity <- repo.saveEntity(DbEntity(0, "hi", 1))
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/state/${dbEntity.id}",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(toUnknown.asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.NotFound
        //TODO: check error
      }
    }
    "entity not found" in {
      for {
        _ <- repo.saveEntity(DbEntity(0, "hi", 1))
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/state/10",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(toUnknown.asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.NotFound
        //TODO: check error
      }
    }
    "trying to move to state which isn't related with original entity state" in {
      for {
        dbEntity <- repo.saveEntity(DbEntity(0, "hi", 1))
        _        <- repo.saveOrUpdateState(UpdateStateRequest("temp", Seq.empty, Seq.empty))
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/state/${dbEntity.id}",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(ChangeStateRequest("temp").asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.BadRequest
        //TODO: check error
      }
    }
    "happy path" in {
      for {
        dbEntity <- repo.saveEntity(DbEntity(0, "hi", 1))
        _        <- repo.saveOrUpdateState(UpdateStateRequest("temp", Seq("init"), Seq("pending")))
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/state/${dbEntity.id}",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(ChangeStateRequest("temp").asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.OK
      }
    }
  }
}
