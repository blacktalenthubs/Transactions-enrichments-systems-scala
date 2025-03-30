package com.payment.merchants

import org.apache.spark.sql.{Dataset, SparkSession}

object Concepts {

  case class Transaction(txId:String,amount:Double,status:String)


  def statusMessage(status: String): String = status match {
    case "COMPLETED" => "All good!"
    case "DECLINED"  => "Payment was declined."
    case "PENDING"   => "Awaiting further action."
    case other       => s"Unknown status: $other"
  }

  println(statusMessage("COMPLETED"))

  // "All good!"

  case class Merchant(
                       merchantId: String,
                       merchantName: String,
                       status: String,
                       riskScore: BigDecimal
                     )

  // Instantiating a Merchant
  val m = Merchant("M1001", "Amazon", "ACTIVE", BigDecimal("5.4"))

  // Pattern matching on Merchant
  m match {
    case Merchant(id, name, "ACTIVE", _) => println(s"Active merchant: $name (ID: $id)")
    case _                               => println("Some other merchant status.")
  }

  def main(args:Array[String]): Unit  = {
    val spark = SparkSession.builder()
      .appName("SparkLocalDebug")
      .master("local[*]")
      // Large number of shuffle partitions => bigger shuffle overhead
      .config("spark.sql.shuffle.partitions", "400")
      // Force Spark NOT to auto-broadcast small tables, so we see a SortMergeJoin shuffle
      .config("spark.sql.autoBroadcastJoinThreshold", "-1")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    spark.sparkContext.setLogLevel("WARN")

    val trans: List[Transaction] = List (
      Transaction("txn_abc123", 10.50, "COMPLETED") ,
      Transaction("txn_def456", 200.00, "COMPLETED"),
      Transaction("txn_ghi789", 55.00, "PENDING")
    )


    val completed  = trans.filter(tx => tx.status== "COMPLETED")
    val map_to_id = completed.map(row=> row.txId)
    println(completed.toString())
    println(map_to_id)

    val merchants: Seq[String] = Seq("Amazon", "Target", "Walmart")

    val transformation = merchants.map(record => record.toLowerCase)

    println(transformation)


    import spark.implicits._
    val txDF = spark.read.parquet("/Volumes/dev/transactions-enrichment-systems/data/transactions")
    txDF.show()

//    val txDS: Dataset[Transactions] = txDF.as[Transactions]
//
//    println(txDS)

    val filtered = txDF.filter($"status" === "COMPLETED").select($"transaction_id",$"amount",$"status")
    filtered.show()
    filtered.explain(true)
  }
}
