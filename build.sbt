name := "dst-lib"

organization := "se.destination"

version := "1.7"

libraryDependencies ++= Seq(
  "commons-net" % "commons-net" % "3.3",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-io" % "commons-io" % "2.4"
)

scalacOptions ++= Seq(
  "-feature"
)

play.Project.playScalaSettings
