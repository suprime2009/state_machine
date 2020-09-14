import sbt.Keys.scalacOptions
import sbt._

lazy val machine =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .enablePlugins(JavaAppPackaging)
    .configs(IntegrationTest)
    .settings(settings)
    .settings(
      organization := "com.pavlo",
      name := "machine",
      dockerSettings,
      addCommandAlias("docker", ";machine/docker"),
      mainClass in Compile := Some("com.pavlo.AppRunner"),
      Defaults.itSettings,
      headerSettings(IntegrationTest),
      inConfig(IntegrationTest)(scalafmtSettings),
      IntegrationTest / console / scalacOptions --= Seq("-Xfatal-warnings", "-Ywarn-unused-import"),
      IntegrationTest / parallelExecution := false,
      IntegrationTest / unmanagedSourceDirectories := Seq((IntegrationTest / scalaSource).value)
    )
    .settings(
      libraryDependencies ++= Seq(
        library.akkaHttp,
        library.akkaHttpJson,
        library.akkaSlf4j,
        library.sprayJson,
        library.akkaStream,
        library.catsCore,
        library.catsEffect,
        library.circeCore,
        library.circeGeneric,
        library.circeRefined,
        library.circeParser,
        library.flywayCore,
        library.slick,
        library.slickHikariCP,
        library.logback,
        library.mysql,
        library.pureConfig,
        library.refinedCats,
        library.refinedCore,
        library.refinedPureConfig,
        library.scalaLogging,
        library.refinedScalaCheck % IntegrationTest,
        library.scalaCheck % IntegrationTest,
        library.scalaTest % IntegrationTest,
        library.akkaStreamTestkit % Test,
        library.refinedScalaCheck % Test,
        library.akkaHttpTestkit % IntegrationTest,
        library.testContainersMysql % IntegrationTest,
        library.testContainersScala % IntegrationTest,
        library.scalaCheck % Test,
        library.scalaTest % Test
      )
    )

lazy val library =
  new {
    object Version {
      val akka = "2.5.25"
      val akkaHttp = "10.1.10"
      val akkaHttpJson = "1.29.1"
      val cats = "2.1.1"
      val catsEffectVersion = "2.1.4"
      val circe = "0.11.1"
      val flyway = "6.0.1"
      val logback = "1.2.3"
      val mysql = "8.0.18"
      val pureConfig = "0.11.1"
      val refined = "0.9.9"
      val scalaCheck = "1.14.0"
      val scalaTest = "3.0.8"
      val slick = "3.3.2"
      val testContainersVersion = "1.12.2"
      val testContainersScalaVersion = "0.33.0"
    }

    val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
    val akkaHttpJson = "de.heikoseeberger" %% "akka-http-circe" % Version.akkaHttpJson
    val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp
    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.akka
    val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka

    val sprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Version.akkaHttp

    val catsCore = "org.typelevel" %% "cats-core" % Version.cats
    val catsEffect = "org.typelevel" %% "cats-effect" % Version.catsEffectVersion
    val circeCore = "io.circe" %% "circe-core" % Version.circe
    val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
    val circeRefined = "io.circe" %% "circe-refined" % Version.circe
    val circeParser = "io.circe" %% "circe-parser" % Version.circe

    val slick = "com.typesafe.slick" %% "slick" % Version.slick
    val slickHikariCP = "com.typesafe.slick" %% "slick-hikaricp" % Version.slick
    val testContainersMysql = "org.testcontainers" % "mysql" % Version.testContainersVersion
    val testContainersScala = "com.dimafeng" %% "testcontainers-scala" % Version.testContainersScalaVersion excludeAll ExclusionRule(
      organization = "org.jetbrains"
    )
    val flywayCore = "org.flywaydb" % "flyway-core" % Version.flyway

    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    val logback = "ch.qos.logback" % "logback-classic" % Version.logback
    val mysql = "mysql" % "mysql-connector-java" % Version.mysql
    val pureConfig = "com.github.pureconfig" %% "pureconfig" % Version.pureConfig
    val refinedCore = "eu.timepit" %% "refined" % Version.refined
    val refinedCats = "eu.timepit" %% "refined-cats" % Version.refined
    val refinedPureConfig = "eu.timepit" %% "refined-pureconfig" % Version.refined
    val refinedScalaCheck = "eu.timepit" %% "refined-scalacheck" % Version.refined
    val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
    val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  }

lazy val settings =
  commonSettings ++
    scalafmtSettings

lazy val commonSettings =
  Seq(
    version := sys.env.getOrElse("VERSION", "0.1.0"),
    scalaVersion := "2.12.10",
    crossScalaVersions := Seq(scalaVersion.value, "2.13.1"),
    organizationName := "Pavlo Kravets",
    startYear := Some(2020),
    headerLicense := Some(HeaderLicense.Custom(licenseText)),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    Compile / console / scalacOptions --= Seq("-Xfatal-warnings", "-Ywarn-unused-import"),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / console / scalacOptions --= Seq("-Xfatal-warnings", "-Ywarn-unused-import"),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
  )

val licenseText = s"""For some reason docker:publishLocal fails due to missing license""".stripMargin

lazy val dockerSettings = Seq(
  dockerfile in docker := {
    val artifact: File     = assembly.value
    val artifactTargetPath = s"/app/${artifact.name}"

    new Dockerfile {
      from("openjdk:8-jre")
      add(artifact, artifactTargetPath)
      entryPoint("java", "-jar", artifactTargetPath)
    }
  },
  imageNames in docker := Seq(
    ImageName(
      namespace = Some(organization.value),
      repository = name.value,
      tag = Some(version.value)
    )
  )
)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

