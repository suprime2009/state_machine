package com.pavlo.test

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, _}
import akka.util.ByteString
import com.pavlo.BaseUseCaseSpec
import com.pavlo.db.DbEntity
import com.pavlo.web.{Codecs, SaveEntityRequest}
import io.circe.syntax._

import scala.collection.immutable.Seq

class AddEntitySpec extends BaseUseCaseSpec with Codecs {
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

  "Saving entity" when {
    val saveRequest = SaveEntityRequest("hi")
    "entity already exists" in {
      for {
        _ <- repo.saveEntity(DbEntity(0, "hi", 1))
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/entity",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(saveRequest.asJson.noSpaces)
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
        resp <- http.singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = s"$baseUrl/entity",
            headers = Seq(),
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              data = ByteString(saveRequest.asJson.noSpaces)
            )
          )
        )
      } yield {
        resp.status mustBe StatusCodes.OK
        //TODO: check body
      }
    }
  }
}
