Build using sbt assembly

Then, either:

i) Use spark-submit:

    bin/spark-submit --master <spark master url> --name Analytics --deploy-mode cluster --class <class name> <assembly jar> <program arguments>*

    e.g. on my machine:

    bin/spark-submit --master spark://Defaults-MacBook-Pro-3.local:7077 --name Analytics --deploy-mode cluster --class org.alfresco.analytics.PopularContent /Users/sglover/src/git/hon/dev/analytics/target/scala-2.11/AlfrescoAnalytics-assembly-0.1-SNAPSHOT.jar /Users/sglover/src/git/hon/dev/analytics/target/scala-2.11/AlfrescoAnalytics-assembly-0.1-SNAPSHOT.jar /Users/sglover/src/git/hon/data

ii) or, run the class "org.alfresco.analytics.SparkSubmit" (which calls spark-submit) with the following arguments:

    <spark master url> <spark home> <assembly jar> <class name> <program arguments>*

    e.g. on my machine:

    spark://Defaults-MacBook-Pro-3.local:7077 /Users/sglover/dev/spark-1.5.2 /Users/sglover/src/git/hon/dev/analytics/target/scala-2.11/AlfrescoAnalytics-assembly-0.1-SNAPSHOT.jar org.alfresco.analytics.PopularContent /Users/sglover/src/git/hon/data