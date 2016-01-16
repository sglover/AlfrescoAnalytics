package org.alfresco.analytics

import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.rdd._

/**
  * Created by sglover on 11/01/2016.
  */
trait Recommendations extends SparkDataSelection {
  this: Sparky =>

  val modelPath = "ratingsModel"

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating)))
      .values
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }

  def buildRatingsModelForYearMonth(yearMonth:String): Unit = {
    val ratingsRDD = userRatingsForYearMonth(yearMonth)
    buildRatingsModel(ratingsRDD)
  }

  def buildRatingsModel(tableName:String): Unit = {
    val ratingsRDD = sqlContext
      .table(tableName)
      .select("username", "nodeId", "siteId", "type", "paths", "mimeType", "timestamp")
      .map {
        r => {
          //            (fields(6).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))
          (r.getAs[Long]("timestamp") % 10, Rating(hashIt(r.getAs[String]("username")), hashIt(r.getAs[String]("nodeId")), r.getAs[Double]("rating")))
        }
      }
      .cache()
    buildRatingsModel(ratingsRDD)
  }

  def buildRatingsModel(ratingsRDD:RDD[(Long, Rating)]): Unit = {
    val ranks = List(8, 12)
    val lambdas = List(1.0, 10.0)
    val numIters = List(10, 20)
    val alpha = 2.0

    val numPartitions = 4
    val trainingRDD = ratingsRDD.filter(x => x._1 < 6)
      .values
      .repartition(numPartitions)
      .cache()
    val validationRDD = ratingsRDD.filter(x => x._1 >= 6 && x._1 < 8)
      .values
      .repartition(numPartitions)
      .cache()
    val testRDD = ratingsRDD.filter(x => x._1 >= 8).values.cache()

    val numTraining = trainingRDD.count()
    val numValidation = validationRDD.count()
    val numTest = testRDD.count()
    var bestValidationRmse = Double.MaxValue
    var bestModel: Option[MatrixFactorizationModel] = None
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1

    for (rank <- ranks; lambda <- lambdas; numIter <- numIters) {
      val model = ALS.train(trainingRDD, rank, numIter, lambda)
      val validationRmse = computeRmse(model, validationRDD, numValidation)
      println("RMSE (validation) = " + validationRmse + " for the model trained with rank = "
        + rank + ", lambda = " + lambda + ", and numIter = " + numIter + ".")
      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }

    bestModel.get.save(sc, modelPath)

    // evaluate the best model on the test set

    val testRmse = computeRmse(bestModel.get, testRDD, numTest)

    println("The best model was trained with rank = " + bestRank + " and lambda = " + bestLambda
      + ", and numIter = " + bestNumIter + ", and its RMSE on the test set is " + testRmse + ".")

    // create a naive baseline and compare it with the best model

    val meanRating = trainingRDD.union(validationRDD).map(_.rating).mean
    val baselineRmse =
      math.sqrt(testRDD.map(x => (meanRating - x.rating) * (meanRating - x.rating)).mean)
    val improvement = (baselineRmse - testRmse) / baselineRmse * 100
    println("The best model improves the baseline by " + "%1.2f".format(improvement) + "%.")
  }

  def recommend(userId:String, ratingsRDD:RDD[Rating]): Array[Rating] = {
    // make personalized recommendations

    val bestModel = getRatingsModel()

    val userRatingsRDD:RDD[Rating] = userRatings(userId)

    // candidates == all ratings excluding those for userId
    val candidates = ratingsRDD.map(x => (x.user, x)).leftOuterJoin(userRatingsRDD.map(y => (y.user, y)))
      .map { x => (x._1, x._2._1.product) }
      .cache()

    bestModel
      .predict(candidates)
      .collect()
      .sortBy(-_.rating)
      .take(5)
  }

  def getRatingsModel(): MatrixFactorizationModel = {
    MatrixFactorizationModel.load(sc, modelPath)
  }
}
