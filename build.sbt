val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.12.7"
)

val akkaVersion = "2.5.18"
val akkaGroupId = "com.typesafe.akka"
val akkaActor = akkaGroupId %% "akka-actor" % akkaVersion
val akkaActorTestKit = akkaGroupId %% "akka-testkit" % akkaVersion % Test

// TODO: would like to remove the remote dep as well?
val akkaRemote = akkaGroupId %% "akka-remote" % akkaVersion
val akkaStream = akkaGroupId %% "akka-stream" % akkaVersion
val akkaStreamTestKit = akkaGroupId %% "akka-stream-testkit" % akkaVersion % Test
val akkaMultiNodeTestKit = akkaGroupId %% "akka-multi-node-testkit" % akkaVersion

val scalaTestVersion = "3.0.5"
val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test

val scalaPb = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

val deps = Seq(akkaActor, akkaActorTestKit, akkaRemote, akkaStream, akkaStreamTestKit, akkaMultiNodeTestKit, scalaTest, scalaPb)

lazy val `akka-hyparview` = (project in file("."))
  .settings(
    commonSettings,
    libraryDependencies ++= deps,
    PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)