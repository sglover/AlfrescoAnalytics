/*assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) { (old) =>
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
}*/

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {_.data.getName == "guava-16.0.1.jar"}
}