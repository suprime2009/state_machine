package com.pavlo

import java.net.ServerSocket

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.testkit.TestKit
import com.pavlo.BaseUseCaseActor.BaseUseCaseActorCmds
import com.pavlo.db.Repository
import com.pavlo.service.Service
import com.pavlo.web.WebRoute
import com.typesafe.config._
import org.flywaydb.core.Flyway
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import slick.basic._
import slick.jdbc._

import scala.concurrent.duration._

abstract class BaseUseCaseSpec
    extends TestKit(
      ActorSystem(
        "it-test",
        ConfigFactory
          .parseString(s"api.port=${BaseUseCaseSpec.findAvailablePort()}")
          .withFallback(ConfigFactory.load())
      )
    )
    with AsyncWordSpecLike
    with MustMatchers
    with ScalaCheckPropertyChecks
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  final val baseUrl: String = s"""http://${system.settings.config
    .getString("api.host")}:${system.settings.config
    .getInt("api.port")}"""

  private val url = "jdbc:mysql://" +
    system.settings.config.getString("database.db.properties.serverName") +
    ":" + system.settings.config
      .getString("database.db.properties.portNumber") +
    "/" + system.settings.config
      .getString("database.db.properties.databaseName")
  private val user = system.settings.config.getString("database.db.properties.user")
  private val pass =
    system.settings.config.getString("database.db.properties.password")
  protected val flyway: Flyway = Flyway.configure().dataSource(url, user, pass).load()

  protected val dbConfig: DatabaseConfig[JdbcProfile] =
    DatabaseConfig.forConfig("database", system.settings.config)
  protected val repo = new Repository(dbConfig)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system, FiniteDuration(5, SECONDS))

  override protected def beforeAll(): Unit = {
    val _ = flyway.migrate()
    val a = system.actorOf(BaseUseCaseActor.props(repo, dbConfig, materializer))
    a ! BaseUseCaseActorCmds.Start
  }
}

object BaseUseCaseSpec {

  def findAvailablePort(): Int = {
    val serverSocket = new ServerSocket(0)
    val freePort     = serverSocket.getLocalPort
    serverSocket.setReuseAddress(true)
    serverSocket.close()
    freePort
  }
}

final class BaseUseCaseActor(
    repo: Repository,
    dbConfig: DatabaseConfig[JdbcProfile],
    mat: ActorMaterializer
) extends Actor
    with ActorLogging {
  import context.dispatcher

  implicit val system: ActorSystem             = context.system
  implicit val materializer: ActorMaterializer = mat

  override def receive: Receive = {
    case BaseUseCaseActorCmds.Start =>
      val service   = new Service(repo)(dbConfig)
      val webRoutes = new WebRoute(service)
      val routes    = webRoutes.routes
      val host      = context.system.settings.config.getString("api.host")
      val port      = context.system.settings.config.getInt("api.port")
      val _         = Http().bindAndHandle(routes, host, port)
    case BaseUseCaseActorCmds.Stop =>
      context.stop(self)
  }
}

object BaseUseCaseActor {

  def props(
      repo: Repository,
      dbConfig: DatabaseConfig[JdbcProfile],
      mat: ActorMaterializer
  ): Props = Props(new BaseUseCaseActor(repo, dbConfig, mat))

  sealed trait BaseUseCaseActorCmds

  object BaseUseCaseActorCmds {

    case object Start extends BaseUseCaseActorCmds

    case object Stop extends BaseUseCaseActorCmds
  }
}
