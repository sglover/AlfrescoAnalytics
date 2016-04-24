//import sbt.Keys._

organization  := "org.alfresco"

version       := "1.0-SNAPSHOT"

name := "AlfrescoAnalyticsCommon"
scalaVersion  := "2.11.7"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

//resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
//resolvers += "alfresco-public" at "https://artifacts.alfresco.com/nexus/content/groups/public"
//resolvers += "alfresco-internal" at "https://artifacts.alfresco.com/nexus/content/groups/internal"
//resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

publishTo := Some(Resolver.file("file", new File(Path.userHome.asFile.toURI.toURL+"/.m2/repository")))

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

//ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

resolvers ++= Seq(
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases" at "http://scala-tools.org/repo-releases",
  "alfresco" at "https://artefacts.alfresco.com/nexus/content/groups/public",
  "alfresco-internal-snapshots" at "https://artefacts.alfresco.com/nexus/content/repositories/internal-snapshots",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/internal-snapshots/",
  "alfresco-public" at "https://artifacts.alfresco.com/nexus/content/groups/public",
  "alfresco-internal" at "https://artifacts.alfresco.com/nexus/content/groups/internal",
  "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
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
    "com.alfresco" % "micro-transformers-client" % "0.1-SNAPSHOT" excludeAll(
      ExclusionRule(organization = "ch.qos.logback"),
      ExclusionRule(organization = "org.slf4j")),
    "commons-io" % "commons-io" % "2.4",
    "io.spray" %%  "spray-json" % "1.3.2",
    "org.apache.chemistry.opencmis" % "chemistry-opencmis-client-impl" % "0.13.0",
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

//publishTo := Some("Sonatype Snapshots Nexus" at "https://oss.sonatype.org/content/repositories/snapshots")

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

