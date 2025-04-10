package com.payment.merchants

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object TransactionsKafkaConsumerDuplicate {

  //todo yemi to implement fraudSignal consumer
  // Define a schema matching the JSON structure in the topic
  val transactionSchema = new StructType()
    .add("tx_id", StringType)
    .add("user_id", StringType)
    .add("merchant_id", StringType)
    .add("amount", DoubleType)
    .add("currency", StringType)
    .add("status", StringType)
    .add("failure_reason", StringType)
    .add("payment_method", StringType)



  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("TransactionsKafkaConsumer")
      .master("local[*]")
      .config("spark.driver.bindAddress", "127.0.0.1")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    // 1) Read from Kafka
    val rawDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "transactions_topic")
      .option("startingOffsets", "latest")
      .load()


    // 2) Extract the JSON string from 'value' field and parse
    val parsedDF = rawDF
      .selectExpr("CAST(value AS STRING) as json_str")
      .select(from_json(col("json_str"), transactionSchema).as("tx"))
      .select("tx.*") // Flatten the struct


    // 3) Write to console for testing
    val query = parsedDF.writeStream
      .format("console")
      .option("truncate", "false")
      .start()
    query.awaitTermination()
  }
}
