inScope(Global)(Seq(
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USERNAME", ""),
    sys.env.getOrElse("SONATYPE_PASSWORD", "")
  ),
  developers ++= List(
    Developer("gregg@lucidchart.com", "Gregg Hernandez", "", url("https://github.com/gregghz"))
  ),
  homepage := Some(url("https://github.com/lucidsoftware/lucid-android")),
  licenses += "Apache License 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
  organization := "com.lucidchart",
  PgpKeys.pgpPassphrase := Some(Array.emptyCharArray),
  scmInfo := Some(ScmInfo(url("https://github.com/lucidsoftware/lucid-android"), "scm:git:git@github.com:lucidsoftware/lucid-android.git")),
  version := sys.props.getOrElse("build.version", "0-SNAPSHOT")
))

enablePlugins(AndroidLib)

name := "lucid-android"
scalaVersion := "2.11.11"
scalacOptions ++= Seq("-language:experimental.macros", "-deprecation", "-Xlint", "-feature", "-Xfatal-warnings")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "io.estatico" %% "newtype" % "0.1.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
  "org.typelevel" %% "cats-core" % "1.0.0-MF"
)

minSdkVersion in Android := "21"
targetSdkVersion in Android := "26"
platformTarget in Android := "android-26"
buildToolsVersion in Android := Some("26.0.2")
