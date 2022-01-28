import xerial.sbt.Sonatype.GitHubHosting
import com.lightbend.paradox.markdown.Writer

val playVersion      = "2.8.13"
val akkaActorVersion = "2.6.15"
val circeVersion     = "0.14.1"

inThisBuild(List(
  versionPolicyIntention := Compatibility.None,
  organization := "org.endpoints4s",
  sonatypeProjectHosting := Some(
    GitHubHosting("endpoints4s", "play", "julien@richard-foy.fr")
  ),
  homepage := Some(sonatypeProjectHosting.value.get.scmInfo.browseUrl),
  licenses := Seq(
    "MIT License" -> url("http://opensource.org/licenses/mit-license.php")
  ),
  developers := List(
    Developer("julienrf", "Julien Richard-Foy", "julien@richard-foy.fr", url("http://julien.richard-foy.fr"))
  ),
  scalaVersion := "2.13.8",
  crossScalaVersions := Seq("2.13.8", "3.0.2", "2.12.13"),
  versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
))

val `play-server` =
  project
    .in(file("server"))
    .settings(
      name := "play-server",
      publish / skip := scalaVersion.value.startsWith("3"), // Don’t publish Scala 3 artifacts for now because the algebra is not published for Scala 3
      libraryDependencies ++= Seq(
        ("org.endpoints4s" %% "openapi" % "4.0.0").cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-netty-server" % playVersion).cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "algebra-testkit" % "1.0.0" % Test).cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "algebra-circe-testkit" % "1.0.0" % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-test" % playVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion % Test).cross(CrossVersion.for3Use2_13),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13)
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(
            ExclusionRule("org.scala-lang.modules", "scala-xml_3"),
            ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
          )
        } else Nil
      }
    )

val `play-server-circe` =
  project
    .in(file("server-circe"))
    .settings(
      name := "play-server-circe",
      publish / skip := scalaVersion.value.startsWith("3"), // Don’t publish Scala 3 artifacts for now because the algebra is not published for Scala 3
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-parser" % circeVersion,
        ("org.endpoints4s" %% "algebra-circe" % "2.0.0").cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "json-schema-circe" % "2.0.0").cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(`play-server`)

val `play-client` =
  project
    .in(file("client"))
    .settings(
      name := "play-client",
      publish / skip := scalaVersion.value.startsWith("3"), // Don’t publish Scala 3 artifacts for now because the algebra is not published for Scala 3
      libraryDependencies ++= Seq(
        ("org.endpoints4s" %% "openapi" % "4.0.0").cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion).cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "algebra-testkit" % "1.0.0" % Test).cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "algebra-circe-testkit" % "1.0.0" % Test).cross(CrossVersion.for3Use2_13),
        ("org.endpoints4s" %% "json-schema-generic" % "1.6.0" % Test).cross(CrossVersion.for3Use2_13),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13)
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(
            ExclusionRule("org.scala-lang.modules", "scala-xml_3"),
            ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
          )
        } else Nil
      }
    )

val documentation =
  project.in(file("documentation"))
    .enablePlugins(ParadoxMaterialThemePlugin, ParadoxPlugin, ParadoxSitePlugin, ScalaUnidocPlugin)
    .settings(
      publish / skip := true,
      coverageEnabled := false,
      autoAPIMappings := true,
      Compile / paradoxMaterialTheme := {
        val theme = (Compile / paradoxMaterialTheme).value
        val repository =
          (ThisBuild / sonatypeProjectHosting).value.get.scmInfo.browseUrl.toURI
        theme
          .withRepository(repository)
          .withSocial(repository)
          .withCustomStylesheet("snippets.css")
      },
      paradoxProperties ++= Map(
        "version" -> version.value,
        "scaladoc.base_url" -> s".../${(packageDoc / siteSubdirName).value}",
        "github.base_url" -> s"${homepage.value.get}/blob/v${version.value}"
      ),
      paradoxDirectives += ((_: Writer.Context) =>
        org.endpoints4s.paradox.coordinates.CoordinatesDirective
        ),
      ScalaUnidoc / unidoc / scalacOptions ++= Seq(
        "-implicits",
        "-diagrams",
        "-groups",
        "-doc-source-url",
        s"${homepage.value.get}/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath",
        (ThisBuild / baseDirectory).value.absolutePath
      ),
      ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
        `play-server`, `play-server-circe`, `play-client`
      ),
      packageDoc / siteSubdirName := "api",
      addMappingsToSiteDir(
        ScalaUnidoc / packageDoc / mappings,
        packageDoc / siteSubdirName
      )
    )

val play =
  project.in(file("."))
    .aggregate(`play-server`, `play-server-circe`, `play-client`, documentation)
    .settings(
      publish / skip := true
    )

Global / onChangedBuildSource := ReloadOnSourceChanges
