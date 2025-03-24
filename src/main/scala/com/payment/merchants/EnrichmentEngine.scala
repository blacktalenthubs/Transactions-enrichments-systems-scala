package com.payment.merchants
import org.apache.spark.sql.functions._

import org.apache.spark.sql.{DataFrame, SparkSession}

/*
Reads Upstreams data into dataframes and enriches dataset with more datasets and UDFs to produce an enriched outputs
 */
object EnrichmentEngine {


  def readTable(spark:SparkSession,table:String):DataFrame = {
    spark.read.table(table)

  }

  /**
   *
   * @param spark SparkSession to use.
   * @param tableName Fully qualified Iceberg table name.
   * @param condition SQL condition string to select rows (e.g., "paymentStatus = 'PENDING'").
   * @param updateExpression SQL expression for the update (e.g., "paymentStatus = 'COMPLETED'").
   */
  def updateTable(spark: SparkSession, tableName: String, condition: String, updateExpression: String): Unit = {
    spark.sql(s"UPDATE $tableName SET $updateExpression WHERE $condition")
  }

  /**
   * Deletes records from an Iceberg table using Spark SQL.
   *
   * Example: delete records where amount < 1.5.
   *
   * @param spark SparkSession to use.
   * @param tableName Fully qualified Iceberg table name.
   * @param condition SQL condition string to select rows to delete (e.g., "amount < 1.5").
   */
  def deleteRecords(spark: SparkSession, tableName: String, condition: String): Unit = {
    spark.sql(s"DELETE FROM $tableName WHERE $condition")
  }


  def main(args:Array[String]) :Unit = {
    // val sparkSession = DatasetUtils.getSparkSetup
    // sparkSession.sparkContext.setLogLevel("WARN")
        val spark = SparkSession.builder()
      .appName("DebugShowNamespacesAndWrite")
      .config("spark.sql.defaultCatalog", "myGlueCatalog")
      .config("spark.sql.catalog.myGlueCatalog", "org.apache.iceberg.spark.SparkCatalog")
      .config("spark.sql.catalog.myGlueCatalog.catalog-impl", "org.apache.iceberg.aws.glue.GlueCatalog")
      .config("spark.sql.catalog.myGlueCatalog.warehouse", "s3a://iceberg-mentorhub/")
      .config("spark.sql.catalog.myGlueCatalog.io-impl", "org.apache.iceberg.aws.s3.S3FileIO")
      .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.kryoserializer.buffer.max", "2000M")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

  import spark.implicits._
   val trans =  readTable(spark, table = "dev.transactions")


    //.out.println("total transactions record is"+trans.count() )
    trans.show()
    trans.explain(true)

    val computedDF =

      trans.filter($"status"==="REFUNDED")
        .filter($"reason_code".isNull)
        .select($"transaction_id",$"merchant_id",$"currency",$"status",$"payment_method",$"amount",$"reason_code")
        .groupBy("merchant_id")
        .agg(avg($"amount").as("avg_spend"),count("*").as("transaction_count"),sum($"amount").as("sum_amount_per_merchant"))
    computedDF.show()
    computedDF.explain(true)


  }
}
