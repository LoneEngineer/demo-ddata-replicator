import com.typesafe.sbt.packager.docker._

lazy val akkaHttpVersion        = "10.1.3"
lazy val akkaVersion            = "2.5.14"
lazy val akkaManagementVersion  = "0.17.0"

resolvers += Resolver.bintrayRepo("tanukkii007", "maven")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "io.digitalmagic",
      scalaVersion    := "2.12.6"
    )),
    name := "replicator",
    version := "1.7",
    maintainer := "sergei@digital-magic.io",
    dockerBaseImage := "frolvlad/alpine-oraclejdk8",
    dockerExposedPorts := Seq(8080, 8081, 2552),
    daemonUser in Docker := "root",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

      "com.typesafe.akka" %% "akka-distributed-data"  % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster"           % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools"     % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"            % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"             % akkaVersion,

      "com.lightbend.akka.management" %% "akka-management"                    % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http"       % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap"  % akkaManagementVersion,
      "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"      % akkaManagementVersion,
      "com.lightbend.akka.discovery"  %% "akka-discovery-dns"                 % akkaManagementVersion,

      "com.github.TanUkkii007" %% "akka-cluster-custom-downing" % "0.0.12",

      "ch.qos.logback"    %  "logback-classic"      % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    ),
    javaOptions in Universal ++= Seq(
      "-Dconfig.resource=akka.conf",
      "-Dhttp.port=8080",
      "-J-Xmx128m"
    )
  ).enablePlugins(JavaServerAppPackaging, AshScriptPlugin, DockerPlugin)
