val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.12.7"
)

val akkaVersion = "2.5.18"
val akkaGroupId = "com.typesafe.akka"
val akkaActor = akkaGroupId %% "akka-actor" % akkaVersion
val akkaActorTestKit = akkaGroupId %% "akka-testkit" % akkaVersion % Test
val akkaRemote = akkaGroupId %% "akka-remote" % akkaVersion
// TODO: multi-jvm test

val scalaTestVersion = "3.0.5"
val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test

val pureConfigVersion = "0.10.0"
val pureConfig = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion

val deps = Seq(akkaActor, akkaActorTestKit, akkaRemote, scalaTest, pureConfig)

lazy val `akka-hyparview` = (project in file("."))
  .settings(
    commonSettings,
    libraryDependencies ++= deps
  )