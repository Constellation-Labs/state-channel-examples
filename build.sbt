import Dependencies._

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "org.acme"
ThisBuild / organizationName := "acme"

ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / scalafixDependencies += Libraries.organizeImports

resolvers += Resolver.sonatypeRepo("snapshots")

val scalafixCommonSettings = inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest))

lazy val commonSettings = Seq(
  scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info", "-language:reflectiveCalls"),
  scalafmtOnCompile := true,
  scalafixOnCompile := true,
  resolvers ++= List(
    Resolver.sonatypeRepo("snapshots")
  )
)

ThisBuild / assemblyMergeStrategy := {
  case "logback.xml" => MergeStrategy.first
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case PathList(xs @ _*) if xs.last == "module-info.class" => MergeStrategy.first
  case x => (assembly / assemblyMergeStrategy).value(x)
}

Global / fork := true
Global / cancelable := true
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "acme"
  )
  .aggregate(stateChannel)

lazy val stateChannel = (project in file("modules/state-channel"))
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "acme-state-channel",
    Defaults.itSettings,
    scalafixCommonSettings,
    commonSettings,
    libraryDependencies ++= Seq(
      Libraries.http4sDsl,
      Libraries.http4sEmberClient,
      Libraries.tessellationSdk,
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      CompilerPlugin.semanticDB
    )
  )

addCommandAlias("runLinter", ";scalafixAll --rules OrganizeImports")
