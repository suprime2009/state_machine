/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo.web

import akka.http.scaladsl.server.Directives.{complete, path}
import com.pavlo.service.Service
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import com.pavlo.web.ExceptionHandlerDirective._

import scala.concurrent.ExecutionContext

class WebRoute(service: Service)(implicit ec: ExecutionContext) extends Codecs {

  val routes = handleExceptions(exceptionHandler) {
    entityRouts ~ stateRouts
  }

  val entityRouts = path("entity") {
      post {
        entity(as[SaveEntityRequest]) { e =>
          complete {
            service.saveEntity(e)
          }
        }
      } ~ get {
        complete {
          service.selectAllEntities
        }
      }
    } ~ path("state" / IntNumber) { entityId: Int =>
      post {
        entity(as[ChangeStateRequest]) { e =>
          complete {
            service.moveEntity(ChangeStateMessage(e.state, entityId))
          }
        }
      }
    }

  val stateRouts = path("states" / "createOrUpdate") {
      post {
        entity(as[UpdateStateRequest]) { e =>
          complete {
            service.createOrUpdateState(e)
          }
        }
      }
    } ~ path("state" / "all") {
      get {
        complete {
          service.selectAllStates
        }
      }
    }
}
