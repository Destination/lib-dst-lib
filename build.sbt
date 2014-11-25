name := "dst-lib"

organization := "se.destination"

version := "1.4-SNAPSHOT"

publishTo := {
  val repoPath = Path.userHome.absolutePath + "/Dropbox/Destination/dst_maven"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some(Resolver.file("file", new File(repoPath + "/snapshots"))(Resolver.ivyStylePatterns))
  else
    Some(Resolver.file("file", new File(repoPath + "/releases"))(Resolver.ivyStylePatterns))
}

libraryDependencies ++= Seq(
  "commons-net" % "commons-net" % "3.2",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-io" % "commons-io" % "2.4"
)

scalacOptions ++= Seq(
  "-feature"
)

play.Project.playScalaSettings
