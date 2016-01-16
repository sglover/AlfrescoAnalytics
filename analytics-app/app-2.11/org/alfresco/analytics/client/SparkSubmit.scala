package org.alfresco.analytics.client

import java.util.concurrent.TimeUnit

import org.apache.spark.launcher.SparkLauncher
import org.slf4j.LoggerFactory

/**
  * Created by sglover on 23/11/2015.
  */
//spark://Defaults-MacBook-Pro-3.local:7077 /Users/sglover/dev/spark-1.5.2 /Users/sglover/src/git/AlfrescoExtensions/alfresco-extensions-analytics/target/scala-2.11/AlfrescoAnalytics-assembly-0.5.jar org.alfresco.analytics.ProcessLines 123 1 /Users/sglover/Documents/UsingVoltDB.txt
//spark://Defaults-MacBook-Pro-3.local:7077 /Users/sglover/dev/spark-1.5.2 file:/Users/sglover/src/git/AlfrescoExtensions/alfresco-extensions-analytics/target/scala-2.11/AlfrescoAnalytics-assembly-0.5.jar org.alfresco.analytics.PopularSites /Users/sglover/src/git/hon/data/pentaho_di_activity-201506.txt
object SparkSubmit {
  def main(args: Array[String]): Unit = {
    val logger = LoggerFactory.getLogger(SparkSubmit.getClass)

    val master = args(0)
    val sparkHome = args(1)
    val appjar = args(2)
    val mainClass = args(3)
    val args1 = args.drop(4)

    println("master=" + master)

    val launcher = new SparkLauncher()
      .setSparkHome(sparkHome)
      .setAppName("Analytics")
      .setDeployMode("cluster")
      .setAppResource(appjar)
      .setMainClass(mainClass)
      .addSparkArg("--total-executor-cores", "5")
      .addSparkArg("--executor-memory", "1G")
      .setMaster(master)
      .addAppArgs(args1:_*)
      .launch()
//    println("Output")
//    IOUtils.copy(launcher.getInputStream(), System.out)
//    println("Error")
//    IOUtils.copy(launcher.getErrorStream(), System.out)
    if(launcher.waitFor(5, TimeUnit.SECONDS))
    {
      launcher.exitValue()
    }
    else
    {
      -1
    }
  }
}
