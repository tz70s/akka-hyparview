val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.13.0"
)

val akkaVersion = "2.5.23"
val akkaGroupId = "com.typesafe.akka"
val akkaActor = akkaGroupId %% "akka-actor" % akkaVersion
val akkaActorTestKit = akkaGroupId %% "akka-testkit" % akkaVersion % Test
val akkaActorTyped = akkaGroupId %% "akka-actor-typed" % akkaVersion
val akkaActorTestKitTyped = akkaGroupId %% "akka-actor-testkit-typed" % akkaVersion % Test

// TODO: would like to remove the remote dep as well?
val akkaRemote = akkaGroupId %% "akka-remote" % akkaVersion
val akkaStream = akkaGroupId %% "akka-stream" % akkaVersion
val akkaStreamTestKit = akkaGroupId %% "akka-stream-testkit" % akkaVersion % Test
val akkaMultiNodeTestKit = akkaGroupId %% "akka-multi-node-testkit" % akkaVersion

val scalaTestVersion = "3.0.8"
val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test

val scalaPb = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

val akkaLibs = Seq(akkaActor, akkaActorTestKit, akkaActorTyped, akkaActorTestKitTyped, akkaRemote, akkaStream, akkaStreamTestKit, akkaMultiNodeTestKit)
val deps = akkaLibs ++ Seq(scalaTest, scalaPb)

lazy val `akka-hyparview` = (project in file("."))
  .settings(
    commonSettings,
    libraryDependencies ++= deps,
    PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)