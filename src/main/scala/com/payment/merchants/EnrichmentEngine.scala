package com.payment.merchants

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
    val sparkSession = DatasetUtils.getSparkSetup
    sparkSession.sparkContext.setLogLevel("WARN")

    readTable(spark = sparkSession, table = "dev.users").show()
  }
}
