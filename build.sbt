import xerial.sbt.Sonatype.GitHubHosting
import com.lightbend.paradox.markdown.Writer

val playVersion      = "3.0.0"
val circeVersion     = "0.14.6"

inThisBuild(List(
  versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
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
  scalaVersion := "2.13.12",
  crossScalaVersions := Seq(scalaVersion.value, "3.3.1"),
  versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
))

val `play-server` =
  project
    .in(file("server"))
    .settings(
      name := "play-server",
      libraryDependencies ++= Seq(
        "org.endpoints4s" %% "openapi" % "4.4.0",
        "org.playframework" %% "play" % playVersion,

        "org.playframework" %% "play-netty-server" % playVersion % Test,
        "org.endpoints4s" %% "algebra-testkit" % "5.0.0" % Test,
        "org.endpoints4s" %% "algebra-circe-testkit" % "5.0.0" % Test,
        "org.playframework" %% "play-test" % playVersion % Test,
        "org.playframework" %% "play-ahc-ws" % playVersion % Test,
      )
    )

val `play-server-circe` =
  project
    .in(file("server-circe"))
    .settings(
      name := "play-server-circe",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-parser" % circeVersion,
        "org.endpoints4s" %% "algebra-circe" % "2.5.0",
        "org.endpoints4s" %% "json-schema-circe" % "2.5.0",
      )
    )
    .dependsOn(`play-server`)

val `play-client` =
  project
    .in(file("client"))
    .settings(
      name := "play-client",
      libraryDependencies ++= Seq(
        "org.endpoints4s" %% "openapi" % "4.4.0",
        "org.playframework" %% "play-ahc-ws" % playVersion,

        "org.endpoints4s" %% "algebra-testkit" % "5.0.0" % Test,
        "org.endpoints4s" %% "algebra-circe-testkit" % "5.0.0" % Test,
        "org.endpoints4s" %% "json-schema-generic" % "1.11.0" % Test,
      )
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
