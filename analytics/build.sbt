import sbt.Keys._

organization  := "org.alfresco"

version       := "0.1-SNAPSHOT"

scalaVersion  := "2.11.6"
//scalaVersion  := "2.10.5"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

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
//  val sparkV  = "1.5.1"
  val sparkV  = "1.5.2"

  Seq(
//    "com.typesafe.akka" %% "akka-remote" % akkaV             withSources() withJavadoc,
    "org.scalatest"       %%  "scalatest"             % "2.2.4"  % "test",
    "junit"               %   "junit"                 % "4.12"   % "test",
    "com.datastax.spark" %% "spark-cassandra-connector" % "1.5.0-M1" exclude("io.netty", "netty"),
    "org.alfresco.services" % "alfresco-events" % "1.2.5-SNAPSHOT",
    "org.alfresco" % "alfresco-core" % "5.6" exclude("com.sun.xml.bind", "jaxb-impl"),
    "org.apache.spark" %% "spark-sql"  % sparkV % "provided",
    "org.apache.spark"   %% "spark-core"            % sparkV % "provided",
    "org.apache.spark"   %% "spark-launcher"        % sparkV,
    "org.apache.spark" %% "spark-streaming" % sparkV % "provided",
    "com.sclasen" %% "akka-kafka" % "0.1.0" % "compile",
    "org.apache.spark" %% "spark-streaming-kafka" % sparkV % "provided",
//    "org.apache.spark" %% "spark-mllib" % sparkV % "provided",
    "org.apache.spark" %% "spark-mllib" % sparkV % "provided",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.5.0",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.5.0" classifier "models",
    //"com.databricks" %% "spark-csv" % "1.3.0",
    "org.slf4j" % "slf4j-log4j12" % "1.7.5",
    "com.github.tototoshi" %% "scala-csv" % "1.3.0-SNAPSHOT",
    "org.apache.spark" %% "spark-streaming-kafka" % sparkV,
    "org.alfresco" %% "alfrescoanalyticscommon" % "1.0-SNAPSHOT",
    "io.spray" %%  "spray-json" % "1.3.2",
    "com.alfresco" % "micro-transformers-client" % "0.1-SNAPSHOT" excludeAll(
      ExclusionRule(organization = "ch.qos.logback"),
      ExclusionRule(organization = "org.slf4j")),
    "org.apache.chemistry.opencmis" % "chemistry-opencmis-client-impl" % "0.13.0"
  )
}

test in assembly := {}

lazy val root = (project in file(".")).
  settings(
    name := "AlfrescoAnalytics",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.6",
//    scalaVersion := "2.10.5",
    mainClass in Compile := Some("org.alfresco.analytics.AnalyticsSystem")
  )

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.7", "-unchecked",
  "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")

scalacOptions in Test ++= Seq("-Yrangepos")

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-g:vars")

doc in Compile <<= target.map(_ / "none")

publishArtifact in (Compile, packageSrc) := false

logBuffered in Test := false

Keys.fork in Test := false

parallelExecution in Test := false

//Revolver.settings

fork in run := true

enablePlugins(JavaServerAppPackaging)

assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) { (old) => 
{
  case n PathList("META-INF", "ECLIPSEF.INF") => MergeStrategy.first
  case n PathList("META-INF", "ECLIPSEF.RSA") => MergeStrategy.first
  case n PathList("META-INF", "ECLIPSE_.RSA") => MergeStrategy.first
  case n PathList("META-INF", "ECLIPSEF.SF") => MergeStrategy.first
  case n if n.startsWith("log4j.properties") => MergeStrategy.first
  case n if n.startsWith("META-INF/MANIFEST.MF") => MergeStrategy.discard
  case n if n.startsWith("META-INF/NOTICE.txt") => MergeStrategy.discard
  case n if n.startsWith("META-INF/NOTICE") => MergeStrategy.discard
  case n if n.startsWith("META-INF/LICENSE.txt") => MergeStrategy.discard
  case n if n.startsWith("META-INF/LICENSE") => MergeStrategy.discard
  case n if n.startsWith("rootdoc.txt") => MergeStrategy.discard
  case n if n.startsWith("readme.html") => MergeStrategy.discard
  case n if n.startsWith("readme.txt") => MergeStrategy.discard
  case n if n.startsWith("library.properties") => MergeStrategy.discard
  case n if n.startsWith("license.html") => MergeStrategy.discard
  case n if n.startsWith("about.html") => MergeStrategy.discard
  case n if n.startsWith("META-INF/spring.schemas") => MergeStrategy.discard
  case n if n.startsWith("META-INF/spring.handlers") => MergeStrategy.discard
  case n if n.startsWith("META-INF/spring.tooling") => MergeStrategy.last
  case n if n.startsWith("META-INF/spring.factories") => MergeStrategy.discard
  case n if n.startsWith("META-INF/services/org/apache/camel/component.properties") => MergeStrategy.discard
  case n if n.startsWith("META-INF/io.netty.versions.properties") => MergeStrategy.discard
  case n if n.startsWith("META-INF/DEPENDENCIES") => MergeStrategy.discard
  case n if n.startsWith("META-INF/INDEX.LIST") => MergeStrategy.discard
  case n if n.startsWith("reference.conf") => MergeStrategy.discard
  case n if n.startsWith("overview.html") => MergeStrategy.discard
  case n if n.toLowerCase.matches("meta-inf/.*\\.sf$") => MergeStrategy.discard
  case n if n.toLowerCase.endsWith(".sf") => MergeStrategy.discard
  case n if n.toLowerCase.endsWith(".dsa") => MergeStrategy.discard
  case n if n.toLowerCase.endsWith(".rsa") => MergeStrategy.discard
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.first
  case PathList("org", "alfresco", "analytics", xs @ _*) => MergeStrategy.first
  case x => old(x)
  }
}

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {_.data.getName == "guava-16.0.1.jar"}
}
