val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.12.7"
)

val akkaVersion = "2.5.18"
val akkaGroupId = "com.typesafe.akka"
val akkaActor = akkaGroupId %% "akka-actor" % akkaVersion
val akkaActorTestKit = akkaGroupId %% "akka-testkit" % akkaVersion % Test
val akkaRemote = akkaGroupId %% "akka-remote" % akkaVersion
val akkaMultiNodeTestKit = akkaGroupId %% "akka-multi-node-testkit" % akkaVersion

val scalaTestVersion = "3.0.5"
val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test

val deps = Seq(akkaActor, akkaActorTestKit, akkaRemote, scalaTest, akkaMultiNodeTestKit)

lazy val `akka-hyparview` = (project in file("."))
  .settings(
    commonSettings,
    libraryDependencies ++= deps
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)