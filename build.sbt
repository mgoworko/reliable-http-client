import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._
import ReleaseTransformations._

val defaultScalaVersion = "3.3.5"
val scalaVersions = Seq("2.13.16", defaultScalaVersion)

val commonSettings =
  Seq(
    organization  := "org.rhttpc",
    scalaVersion  := defaultScalaVersion,
    crossScalaVersions := scalaVersions,
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-encoding", "utf8",
      "-feature",
      "-Xfatal-warnings",
      "-language:postfixOps"),
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    headerLicense := Some(HeaderLicense.Custom(
      """|Copyright 2015 the original author or authors.
         |
         |Licensed under the Apache License, Version 2.0 (the "License");
         |you may not use this file except in compliance with the License.
         |You may obtain a copy of the License at
         |
         |    http://www.apache.org/licenses/LICENSE-2.0
         |
         |Unless required by applicable law or agreed to in writing, software
         |distributed under the License is distributed on an "AS IS" BASIS,
         |WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         |See the License for the specific language governing permissions and
         |limitations under the License.
         |""".stripMargin
    )),
    headerEmptyLine := false,
    homepage := Some(url("https://github.com/arkadius/reliable-http-client")),
    dockerRepository := Some("arkadius"),
    dockerBaseImage := "amazoncorretto:11",
    resolvers ++= Seq(
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      Resolver.jcenterRepo
    ),
  )

val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  Test / publishArtifact := false,
  Global / pomExtra := {
    <scm>
      <connection>scm:git:github.com/arkadius/reliable-http-client.git</connection>
      <developerConnection>scm:git:git@github.com:arkadius/reliable-http-client.git</developerConnection>
      <url>github.com/arkadius/reliable-http-client</url>
    </scm>
    <developers>
      <developer>
        <id>ark_adius</id>
        <name>Arek Burdach</name>
        <url>https://github.com/arkadius</url>
      </developer>
    </developers>
  }
)

val pekkoV            = "1.1.3"
val pekkoHttpV        = "1.1.0"
val amqpcV            = "3.6.6"
val betterFilesV      = "3.9.2"
val commonsIoV        = "2.18.0"
val dispatchV         = "2.0.0"
val ficusV            = "1.5.2"
val flywayV           = "6.2.4"
val hsqldbV           = "2.7.4"
val jaxbV             = "2.3.1"
val logbackV          = "1.5.16"
val scalaTestV        = "3.2.19"
val slf4jV            = "1.7.36"
val slickV            = "3.5.2"
val scalaStmV         = "0.11.1"
val testContainersV   = "0.41.8"

lazy val transport = (project in file("rhttpc-transport")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "rhttpc-transport",
    libraryDependencies ++= {
      Seq(
        "org.apache.pekko"         %% "pekko-actor"                   % pekkoV,
        "org.slf4j"                 % "slf4j-api"                     % slf4jV,
        "org.scala-stm"            %% "scala-stm"                     % scalaStmV,
        "org.scalatest"            %% "scalatest"                     % scalaTestV    % "test"
      )
    }
  )

lazy val inMemTransport = (project in file("rhttpc-inmem")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "rhttpc-inmem",
    libraryDependencies ++= {
      Seq(
        "org.apache.pekko"         %% "pekko-testkit"                 % pekkoV         % "test",
        "org.scalatest"            %% "scalatest"                     % scalaTestV    % "test",
        "org.apache.pekko"         %% "pekko-slf4j"                   % pekkoV         % "test",
        "ch.qos.logback"            % "logback-classic"               % logbackV      % "test"
      )
    }
  ).
  dependsOn(transport)

lazy val amqpTransport = (project in file("rhttpc-amqp")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "rhttpc-amqp",
    libraryDependencies ++= {
      Seq(
        "org.apache.pekko"         %% "pekko-stream"                  % pekkoV,
        "com.rabbitmq"              % "amqp-client"                   % amqpcV,
        "com.iheart"               %% "ficus"                         % ficusV,
        "org.apache.pekko"         %% "pekko-testkit"                 % pekkoV         % "test",
        "org.scalatest"            %% "scalatest"                     % scalaTestV    % "test",

        "org.dispatchhttp"         %% "dispatch-core"                 % dispatchV     % "test",
        "org.apache.pekko"         %% "pekko-slf4j"                   % pekkoV         % "test",
        "ch.qos.logback"            % "logback-classic"               % logbackV      % "test",
        "org.apache.pekko"         %% "pekko-http"                    % pekkoHttpV     % "test"
      )
    }
  ).
  dependsOn(transport)

lazy val amqpJdbcTransport = (project in file("rhttpc-amqp-jdbc")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "rhttpc-amqp-jdbc",
    libraryDependencies ++= {
      Seq(
        "com.typesafe.slick"       %% "slick"                         % slickV,
        "org.flywaydb"              % "flyway-core"                   % flywayV       % "optional",
        "org.scalatest"            %% "scalatest"                     % scalaTestV    % "test",
        "com.typesafe.slick"       %% "slick-hikaricp"                % slickV        % "test",
        "org.hsqldb"                % "hsqldb"                        % hsqldbV       % "test",
        "ch.qos.logback"            % "logback-classic"               % logbackV      % "test"
      )
    }
  ).
  dependsOn(amqpTransport)

lazy val client = (project in file("rhttpc-client")).
  settings(commonSettings).
  settings(publishSettings).
  settings(
    name := "rhttpc-client",
    libraryDependencies ++= {
      Seq(
        "com.iheart"               %% "ficus"                         % ficusV,
        "org.apache.pekko"         %% "pekko-testkit"                 % pekkoV         % "test",
        "org.scalatest"            %% "scalatest"                     % scalaTestV    % "test",
        "org.apache.pekko"         %% "pekko-slf4j"                   % pekkoV         % "test",
        "ch.qos.logback"            % "logback-classic"               % logbackV      % "test"
      )
    }
  ).
  dependsOn(transport).
  dependsOn(inMemTransport % "test")

lazy val sampleEcho = (project in file("sample/sample-echo")).
  settings(commonSettings).
  enablePlugins(DockerPlugin).
  enablePlugins(JavaAppPackaging).
  settings(
    libraryDependencies ++= {
      Seq(
        "org.apache.pekko"         %% "pekko-http"                    % pekkoHttpV,
        "org.apache.pekko"         %% "pekko-slf4j"                   % pekkoV,
        "org.apache.pekko"         %% "pekko-stream"                  % pekkoV,
        "ch.qos.logback"            % "logback-classic"               % logbackV,
        "org.scalatest"            %% "scalatest"                     % scalaTestV    % "test"
      )
    },
    dockerExposedPorts := Seq(8082),
    publish / skip := true
  )
  .dependsOn(transport)

lazy val testProj = (project in file("sample/test")).
  settings(commonSettings).
  settings(
    libraryDependencies ++= {
      Seq(
        "com.github.pathikrit"     %% "better-files"                  % betterFilesV,
        "commons-io"                % "commons-io"                    % commonsIoV,
        "org.dispatchhttp"         %% "dispatch-core"                 % dispatchV,
        "javax.xml.bind"            % "jaxb-api"                      % jaxbV,
        "ch.qos.logback"            % "logback-classic"               % logbackV,
        "com.dimafeng"             %% "testcontainers-scala-scalatest" % testContainersV % "test",
        "com.dimafeng"             %% "testcontainers-scala-rabbitmq" % testContainersV % "test",
        "org.scalatest"            %% "scalatest"                     % scalaTestV    % "test"
      )
    },
    Test / Keys.test  := (Test / Keys.test).dependsOn(
      sampleEcho / Docker / publishLocal,
    ).value,
    publish / skip := true
  )

lazy val root = (project in file("."))
  .aggregate(
    transport, inMemTransport, amqpTransport, amqpJdbcTransport,
    client,
    sampleEcho, testProj)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    // crossScalaVersions must be set to Nil on the aggregating project
    releaseCrossBuild := true,
    publish / skip := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      ReleaseStep { st: State =>
        if (!st.get(ReleaseKeys.skipTests).getOrElse(false)) {
          releaseStepCommandAndRemaining("+test")(st)
        } else {
          st
        }
      },
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
      pushChanges
    )
  )
