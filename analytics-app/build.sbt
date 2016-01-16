import sbt.Keys._

organization  := "org.alfresco"

name := "analytics-app"

version       := "0.1-SNAPSHOT"

scalaVersion  := "2.11.6"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

// For stable releases
resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += Resolver.mavenLocal //"Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
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
  val akkaV  = "2.3.11"
  val sparkV  = "1.5.2"

  Seq(
    "com.typesafe.akka" %% "akka-remote" % akkaV             withSources() withJavadoc,
    "com.typesafe.akka"   %%  "akka-testkit"          % akkaV    % "test",
    "org.scalatest"       %%  "scalatest"             % "2.2.4"  % "test",
    "junit"               %   "junit"                 % "4.12"   % "test",
    "org.specs2"          %%  "specs2"                % "2.4.17" % "test", // until spray-testkit gets compiled against specs 3.3
    "ch.qos.logback"      %   "logback-classic"       % "1.1.3",
    "org.springframework.scala" % "spring-scala" % "1.0.0.M2",
    "org.mongodb"       %%  "casbah"            % "2.8.2",
    "com.typesafe.akka"   %% "akka-camel"             % akkaV             withSources() withJavadoc,
    "org.apache.camel" % "camel-jms" % "2.11.0",
    "org.apache.camel" % "camel-jackson" % "2.11.0",
    "org.apache.activemq" % "activemq-core" % "5.7.0",
    "org.alfresco.services" % "alfresco-events" % "1.2.5-SNAPSHOT",
    "org.apache.activemq" % "activemq-camel" % "5.8.0",
    "org.gytheio" % "gytheio-messaging-commons" % "0.3",
    "org.alfresco" % "alfresco-core" % "5.6",
    "org.apache.spark"   %% "spark-launcher" % sparkV,
    "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.9",
    "org.apache.kafka" %% "kafka" % "0.8.2.2" exclude("javax.jms", "jms") exclude("com.sun.jdmk", "jmxtools")
        exclude("com.sun.jmx", "jmxri"),
//    "org.apache.kafka" %% "kafka" % "0.9.0.0" exclude("javax.jms", "jms") exclude("com.sun.jdmk", "jmxtools")
//      exclude("com.sun.jmx", "jmxri"),
    "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.9.0",
    "com.sclasen" %% "akka-kafka" % "0.1.0" % "compile",
    "com.typesafe.akka" %% "akka-slf4j" % "2.3.9" % "compile",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.10" % "compile",
    "org.alfresco" %% "alfrescoanalyticscommon" % "1.0-SNAPSHOT",
    "org.apache.chemistry.opencmis" % "chemistry-opencmis-client-impl" % "0.13.0",
    "org.springframework.social" % "spring-social-alfresco-bm" % "0.5.5-SNAPSHOT",
    "com.alfresco" % "micro-transformers-client" % "0.1-SNAPSHOT" excludeAll(
      ExclusionRule(organization = "ch.qos.logback"),
      ExclusionRule(organization = "org.slf4j")),
    "edu.stanford.nlp" % "stanford-corenlp" % "3.5.0",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.5.0" classifier "models",
    "joda-time" % "joda-time" % "2.8.1"
  )
}

//test in assembly := {}

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.7", "-unchecked",
  "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")

scalacOptions in Test ++= Seq("-Yrangepos")

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-g:vars")

doc in Compile <<= target.map(_ / "none")

//publishArtifact in (Compile, packageSrc) := false

logBuffered in Test := false

Keys.fork in Test := false

parallelExecution in Test := false

//Revolver.settings

fork in run := true

//enablePlugins(JavaServerAppPackaging)


