package com.pavlo.test

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, _}
import akka.util.ByteString
import com.pavlo.BaseUseCaseSpec
import com.pavlo.db.DbEntity
import com.pavlo.web.{ChangeStateRequest, Codecs, UpdateStateRequest}
import io.circe.syntax._

import scala.collection.immutable.Seq

class ModifyStateSpec extends BaseUseCaseSpec with Codecs {
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

  "add new state" when {
    "state fromState contains name of the state for adding" in {
      for {
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/states/createOrUpdate",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(UpdateStateRequest("hi", Seq("init", "hi"), Seq.empty).asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.BadRequest
        //TODO: check error
      }
    }
    "from state not found in DB" in {
      for {
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/states/createOrUpdate",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(UpdateStateRequest("hi", Seq("init", "absentState"), Seq.empty).asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.NotFound
        //TODO: check error
      }
    }
    "to state not found in DB" in {
      for {
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/states/createOrUpdate",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(UpdateStateRequest("hi", Seq("init"), Seq("absentState")).asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.NotFound
        //TODO: check error
      }
    }
    "created state without transitions" in {
      for {
        _ <- repo.saveOrUpdateState(UpdateStateRequest("temp", Seq.empty, Seq.empty))
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/states/createOrUpdate",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(UpdateStateRequest("hi", Seq.empty, Seq.empty).asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.OK
        //TODO: check error
      }
    }
    "created state with multiple transitions" in {
      for {
        _ <- repo.saveOrUpdateState(UpdateStateRequest("temp", Seq.empty, Seq.empty))
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/states/createOrUpdate",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(UpdateStateRequest("hi", Seq("pending", "finished"), Seq("init", "closed")).asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.OK
        //TODO: check error
      }
    }
    "Add transitions to existing state" in {
      for {
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/states/createOrUpdate",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(UpdateStateRequest("init", Seq("closed"), Seq("pending", "finished")).asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.OK
        //TODO: check error
      }
    }
  }
}
