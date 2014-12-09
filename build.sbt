name := "dst-lib"

organization := "se.destination"

version := "1.6-SNAPSHOT"

libraryDependencies ++= Seq(
  "commons-net" % "commons-net" % "3.2",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-io" % "commons-io" % "2.4"
)

scalacOptions ++= Seq(
  "-feature"
)

play.Project.playScalaSettings
