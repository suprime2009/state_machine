/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.pavlo.db.Repository
import com.pavlo.service.Service
import com.pavlo.web.WebRoute
import com.pavlo.web.ExceptionHandlerDirective._
import org.flywaydb.core.Flyway
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object AppRunner {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    val url = "jdbc:mysql://" +
      system.settings.config.getString("database.db.properties.serverName") +
      ":" + system.settings.config.getString("database.db.properties.portNumber") +
      "/" + system.settings.config.getString("database.db.properties.databaseName")
    val user = system.settings.config.getString("database.db.properties.user")
    val pass = system.settings.config.getString("database.db.properties.password")
    val flyway: Flyway = Flyway.configure().dataSource(url, user, pass).load()
    val a = flyway.migrate()
    print(a)

    val dbConfig: DatabaseConfig[JdbcProfile] =
      DatabaseConfig.forConfig("database", system.settings.config)

    val repo = new Repository(dbConfig)

    val service = new Service(repo)(dbConfig)

    val web = new WebRoute(service)

    val routes = web.routes

    val host = system.settings.config.getString("api.host")
    val port = system.settings.config.getInt("api.port")
    val srv = Http().bindAndHandle(routes, host, port)
    val pressEnter = StdIn.readLine()
    srv.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}
