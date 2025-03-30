package com.example.sparkdebug

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import scala.util.Random
import scala.concurrent.duration._

/**
 * A demo job that intentionally introduces common performance pitfalls, now modified
 * to run longer so you can observe the Spark UI for each step:
 *
 *  1) Data Skew
 *  2) Excessive repartition
 *  3) Non-broadcast dimension joins
 *  4) groupByKey usage
 *  5) Repeated transformations w/o caching
 *  6) Large shuffle from unpruned columns
 *  7) Over-partitioning or under-partitioning
 *
 * We generate ~2 million rows, use 400 shuffle partitions, disable auto-broadcast,
 * and insert Thread.sleep(...) calls after each step so you have time to check the UI.
 *
 * Run example:
 *   spark-submit \
 *     --class com.example.sparkdebug.SparkLocalDebug \
 *     --master local[*] \
 *     --driver-memory 4G \
 *     --conf spark.executor.memory=4G \
 *     spark-debug-demo.jar
 *
 * Then open http://localhost:4040 (or the port Spark logs say) to examine Jobs/Stages/Tasks.
 */
object SparkLocalDebug {

  def main(args: Array[String]): Unit = {
    // 1) Build SparkSession (local mode).
    val spark = SparkSession.builder()
      .appName("SparkLocalDebug")
      .master("local[*]")
      // Large number of shuffle partitions => bigger shuffle overhead
      .config("spark.sql.shuffle.partitions", "400")
      // Force Spark NOT to auto-broadcast small tables, so we see a SortMergeJoin shuffle
      .config("spark.sql.autoBroadcastJoinThreshold", "-1")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    println("[INFO] Generating 2,000,000 skewed transaction rows ...")
    val transactionsDF = generateSkewedTransactions(spark, 2000000)

    println("[INFO] Doing a quick aggregator on the entire dataset to warm up and confirm size:")
    val totalCount = transactionsDF.count()
    println(s"[DEBUG] transactionsDF.count() = $totalCount. (Should be ~2M)\n")

    // 2) Show how repeated transformations cause repeated DAG execution
    println("[INFO] Demonstrating repeatedTransformationPitfall ...")
    repeatedTransformationPitfall(transactionsDF)
    println("[DEBUG] Sleeping 20s so you can examine Spark UI for repeatedTransformationPitfall.")
    Thread.sleep(20.seconds.toMillis)

    // 3) Demonstrate groupByKey vs. Agg (large shuffle if skewed)
    println("[INFO] Demonstrating groupByKeyPitfall ...")
    groupByKeyPitfall(transactionsDF)
    println("[DEBUG] Sleeping 20s for groupByKeyPitfall in UI.")
    Thread.sleep(20.seconds.toMillis)

    // 4) Demonstrate multiple repartitions
    println("[INFO] Demonstrating excessiveRepartitionPitfall ...")
    excessiveRepartitionPitfall(transactionsDF)
    println("[DEBUG] Sleeping 20s for excessiveRepartitionPitfall.")
    Thread.sleep(20.seconds.toMillis)

    // 5) Demonstrate a small dimension table (non-broadcast => big shuffle)
    println("[INFO] Generating small dimension & demonstrating nonBroadcastJoinPitfall ...")
    val smallDimDF = generateSmallDimension(spark)
    nonBroadcastJoinPitfall(transactionsDF, smallDimDF)
    println("[DEBUG] Sleeping 20s for nonBroadcastJoinPitfall in UI.")
    Thread.sleep(20.seconds.toMillis)

    // 6) Demonstrate reading columns we don't need
    println("[INFO] Demonstrating columnPruningPitfall ...")
    columnPruningPitfall(transactionsDF)
    println("[DEBUG] Sleeping 20s for columnPruningPitfall in UI.")
    Thread.sleep(20.seconds.toMillis)

    println(
      s"""
         |[INFO] Finished running 'SparkLocalDebug' job (longer run edition).
         |Now open Spark UI -> http://localhost:4040 (or the port in logs).
         |You have 30s before the job stops, or press Ctrl+C to stop earlier.
         |""".stripMargin
    )
    // Keep Spark alive 30s so you can see final job data in the UI
    Thread.sleep(30.seconds.toMillis)
    spark.stop()
  }

  /**
   * Generate a DataFrame of skewed "transactions".
   *
   * SCHEMA:
   *  - userId: ~70% chance "User_SK" (the "hot" key), 30% random
   *  - amount: 0 .. 500
   *  - category: random from a small set
   * ~2 million rows
   */
  def generateSkewedTransactions(spark: SparkSession, numRows: Int): DataFrame = {
    import spark.implicits._

    val categories = Array("Books", "Grocery", "Electronics", "Clothing", "Shoes")
    val rnd = new Random()

    // We'll build a big Seq of tuples, but watch out for memory -> we do it in parallel or chunked
    // For a local example, you can just do a standard .map, but be mindful of driver memory usage
    val data = (1 to numRows).map { _ =>
      val isSkew = rnd.nextDouble() < 0.70
      val userId = if (isSkew) "User_SK" else s"User_${1000 + rnd.nextInt(9000)}"
      val amount = rnd.nextDouble() * 500
      val cat    = categories(rnd.nextInt(categories.length))
      (userId, amount, cat)
    }

    val rdd = spark.sparkContext.parallelize(data, 8)
    rdd.toDF("userId", "amount", "category")
  }

  /**
   * Pitfall #1: repeatedTransformationPitfall
   * Without caching, each action triggers a full re-computation from the source.
   */
  def repeatedTransformationPitfall(transactionsDF: DataFrame): Unit = {
    import transactionsDF.sparkSession.implicits._
    val filteredDF = transactionsDF
      .filter($"amount" > 200)   // ~some filter
      .withColumn("amount2", $"amount" * 2)

    val countVal = filteredDF.count()
    println(s"[Pitfall] repeatedTransformationPitfall => count of filtered: $countVal")

    val avgVal = filteredDF.agg(avg("amount2")).first().getDouble(0)
    println(s"[Pitfall] repeatedTransformationPitfall => avg(amount2): $avgVal\n")
  }

  /**
   * Pitfall #2: groupByKeyPitfall
   * groupByKey => large shuffle if skewed
   */
  def groupByKeyPitfall(transactionsDF: DataFrame): Unit = {
    import transactionsDF.sparkSession.implicits._
    val typedDS = transactionsDF.as[(String, Double, String)]  // (userId, amount, category)
    val grouped = typedDS.groupByKey{ case (user, amt, cat) => user }

    // sum amounts per user
    val summed = grouped.mapGroups{ (user, iter) =>
      var sumAmt = 0.0
      iter.foreach{ case (_, amt, _) => sumAmt += amt }
      (user, sumAmt)
    }

    val countOfUsers = summed.count()
    println(s"[Pitfall] groupByKey => resultCount (number of unique users): $countOfUsers\n")
  }

  /**
   * Pitfall #3: excessiveRepartitionPitfall
   * Multiple repartitions => extra shuffle steps
   */
  def excessiveRepartitionPitfall(transactionsDF: DataFrame): Unit = {
    val repart1 = transactionsDF.repartition(300, col("userId"))
    val repart2 = repart1.repartition(150)
    val repart3 = repart2.repartition(50)

    // example aggregator so it does a real shuffle
    val sumAmt = repart3.agg(sum("amount")).first().getDouble(0)
    println(s"[Pitfall] excessiveRepartition => sum(amount) after final repart: $sumAmt\n")
  }

  /**
   * Pitfall #4: nonBroadcastJoinPitfall
   * We have a small dimension, but do a normal join => triggers big shuffle
   * because we disabled auto-broadcast => SortMergeJoin with large data
   */
  def nonBroadcastJoinPitfall(transactionsDF: DataFrame, smallDimDF: DataFrame): Unit = {
    // simple join on 'category'
    val joinedDF = transactionsDF.join(smallDimDF, Seq("category"), "left")
    val rowCount = joinedDF.count()
    println(s"[Pitfall] nonBroadcastJoin => final joined row count: $rowCount\n")
  }

  /** Generate a small dimension table. */
  def generateSmallDimension(spark: SparkSession): DataFrame = {
    import spark.implicits._
    val cats = Seq("Books","Grocery","Electronics","Clothing","Shoes","Kitchen","Outdoors")
    val data = cats.map(cat => (cat, s"desc_$cat", Random.nextInt(100)))
    spark.createDataFrame(data).toDF("category", "description", "some_metric")
  }

  /**
   * Pitfall #5: columnPruningPitfall
   * We only need "userId, amount" but do .select("*") => reading all columns
   */
  def columnPruningPitfall(transactionsDF: DataFrame): Unit = {
    val dfAll = transactionsDF.select("*")
    val finalDF = dfAll.agg(avg("amount").as("avg_amt"))
    finalDF.show(10, truncate=false)
  }

}
