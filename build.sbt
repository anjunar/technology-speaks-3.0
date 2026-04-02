import org.scalajs.linker.interface.{ESVersion, ModuleKind}
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.2"

lazy val commonJsSettings = Seq(
  scalaJSLinkerConfig ~= (
    _.withModuleKind(ModuleKind.ESModule)
      .withESFeatures(_.withESVersion(ESVersion.ES2021))
    )
)

lazy val scalaReflect = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("library/scala-reflect"))
  .jsSettings(commonJsSettings)
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )

lazy val scalaReflectJS = scalaReflect.js
lazy val scalaReflectJVM = scalaReflect.jvm

lazy val jfx = (project in file("frontend/jfx"))
  .dependsOn(scalaReflectJS)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.1",
    libraryDependencies += ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13),
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
    Test / logBuffered := false,
    Test / outputStrategy := Some(StdoutOutput)
  )
  .settings(commonJsSettings)

lazy val app = (project in file("frontend/app"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfx)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % Test
  )
  .settings(commonJsSettings)

lazy val root = (project in file("."))
  .aggregate(jfx, app)
  .settings(
    publish / skip := true
  )

lazy val scalaUniverse2 = (project in file("library/scala-universe"))
  .dependsOn(scalaReflectJVM)
  .settings(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "33.5.0-jre",
      "jakarta.enterprise" % "jakarta.enterprise.cdi-api" % "4.1.0",
      "ch.qos.logback" % "logback-classic" % "1.5.32",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "org.slf4j" % "jul-to-slf4j" % "2.0.17",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6"
    )
  )

lazy val jsonMapper2 = (project in file("library/json-mapper"))
  .dependsOn(scalaUniverse2)
  .settings(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "33.5.0-jre",
      "jakarta.json.bind" % "jakarta.json.bind-api" % "3.0.1",
      "jakarta.persistence" % "jakarta.persistence-api" % "3.2.0",
      "jakarta.validation" % "jakarta.validation-api" % "3.1.1",
      "tools.jackson.core" % "jackson-databind" % "3.1.0",
      "tools.jackson.module" %% "jackson-module-scala" % "3.1.0",
      "org.hibernate.orm" % "hibernate-core" % "7.2.6.Final",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )

lazy val system = (project in file("system"))
  .dependsOn(jsonMapper2)
  .settings(
    libraryDependencies ++= Seq(
      "org.springframework.boot" % "spring-boot-starter-web" % "4.0.4",
      "org.springframework.boot" % "spring-boot-starter-mail" % "4.0.4",
      "org.springframework.boot" % "spring-boot-starter-thymeleaf" % "4.0.4",
      "org.springframework.boot" % "spring-boot-starter-data-jpa" % "4.0.4",
      "org.springframework.boot" % "spring-boot-starter-validation" % "4.0.4",
      "org.springframework.boot" % "spring-boot-starter-json" % "4.0.4",
      "org.springframework.boot" % "spring-boot-starter-actuator" % "4.0.4",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.21.2",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.21.2",
      "org.thymeleaf" % "thymeleaf" % "3.1.3.RELEASE",
      "org.hibernate.orm" % "hibernate-core" % "7.2.6.Final",
      "org.hibernate.validator" % "hibernate-validator" % "9.1.0.Final",
      "org.postgresql" % "postgresql" % "42.7.10",
      "com.webauthn4j" % "webauthn4j-core" % "0.31.1.RELEASE"
    )
  )

lazy val domain = (project in file("domain"))
  .dependsOn(system)
  .settings(
    libraryDependencies ++= Seq(
      "com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalatestplus" %% "mockito-5-12" % "3.2.19.0" % Test
    )
  )

lazy val rest = (project in file("rest"))
  .dependsOn(domain)

lazy val application = (project in file("application"))
  .dependsOn(rest)
