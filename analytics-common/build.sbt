//import sbt.Keys._

organization  := "org.alfresco"

version       := "1.0-SNAPSHOT"

name := "AlfrescoAnalyticsCommon"
scalaVersion  := "2.11.6"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

//artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
//  artifact.name + "-" + module.revision + "." + artifact.extension
//}

// For stable releases
resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers ++= Seq(
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases" at "http://scala-tools.org/repo-releases",
  "alfresco" at "https://artefacts.alfresco.com/nexus/content/groups/public",
  "alfresco-internal-snapshots" at "https://artefacts.alfresco.com/nexus/content/repositories/internal-snapshots",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/internal-snapshots/",
  "Spray" at "http://repo.spray.io",
  Resolver.bintrayRepo("websudos", "oss-releases")
)

libraryDependencies ++= {

  Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.14",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.4.4",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.4",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4",
    "org.alfresco.services" % "alfresco-events" % "1.2.5-SNAPSHOT",
    "commons-io" % "commons-io" % "2.4",
    "io.spray" %%  "spray-json" % "1.3.2",
    "joda-time" % "joda-time" % "2.8.1"
  )
}

//exportJars := true

lazy val root = (project in file(".")).
  settings(
    name := "AlfrescoAnalyticsCommon",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.6"
  )

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.7", "-unchecked",
  "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")

scalacOptions in Test ++= Seq("-Yrangepos")

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-g:vars")

doc in Compile <<= target.map(_ / "none")

//publishArtifact in (Compile, packageSrc) := true

logBuffered in Test := false

Keys.fork in Test := false

parallelExecution in Test := false

//Revolver.settings

fork in run := true

