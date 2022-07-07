import sbt._

object Dependencies {

  object V {
    val betterMonadicFor = "0.3.1"
    val kindProjector = "0.13.2"
    val organizeImports = "0.5.0"
    val semanticDB = "4.5.9"
  }

  object Libraries {

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
