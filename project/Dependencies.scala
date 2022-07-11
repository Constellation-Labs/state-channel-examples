import sbt._

object Dependencies {

  object V {
    val betterMonadicFor = "0.3.1"
    val http4s = "0.23.13"
    val kindProjector = "0.13.2"
    val organizeImports = "0.5.0"
    val semanticDB = "4.5.9"
    val tessellation = "0.10.0"
  }

  object Libraries {

    def tessellation(artifact: String) =
      ("org.constellation" %% s"tessellation-${artifact}" % V.tessellation).from(
        s"https://github.com/Constellation-Labs/tessellation/releases/download/v${V.tessellation}/cl-node.jar"
      )

    def http4s(artifact: String) = "org.http4s" %% s"http4s-$artifact" % V.http4s

    val http4sDsl = http4s("dsl")
    val http4sEmberClient = http4s("ember-client")
    val tessellationCore = tessellation("core")
    val tessellationSdk = tessellation("sdk")
    


    // Scalafix rules
    val organizeImports = "com.github.liancheng" %% "organize-imports" % V.organizeImports
  }

  object CompilerPlugin {

    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    )

    val kindProjector = compilerPlugin(
      ("org.typelevel" % "kind-projector" % V.kindProjector).cross(CrossVersion.full)
    )

    val semanticDB = compilerPlugin(
      ("org.scalameta" % "semanticdb-scalac" % V.semanticDB).cross(CrossVersion.full)
    )
  }

}
