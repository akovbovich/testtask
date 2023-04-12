ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

val akkaVersion = "2.8.0"

lazy val root = (project in file("."))
  .settings(name := "TestTask")
  .aggregate(console)

lazy val core = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion
    )
  )

lazy val console = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion
    )
  )
  .dependsOn(core)
