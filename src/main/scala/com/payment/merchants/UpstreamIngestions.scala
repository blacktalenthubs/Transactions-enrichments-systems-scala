package com.payment.merchants

import com.github.javafaker.Faker
import java.util.Locale
import java.time.{LocalDate, ZoneId}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import scala.util.Random

object UpstreamIngestions {

  //todo refactor later
  case class WriteDestinationConfig(
                                     localPath:    String  = "",
                                     icebergTable: String  = ""
                                   )

  // SCHEMAS
  val transactionSchema: StructType = StructType(Seq(
    StructField("transaction_id", StringType, nullable = false),
    StructField("user_id", StringType, nullable = true),
    StructField("merchant_id", StringType, nullable = true),
    StructField("amount", DecimalType(12,2), nullable = true),
    StructField("currency", StringType, nullable = true),
    StructField("status", StringType, nullable = true),
    StructField("reason_code", StringType, nullable = true),
    StructField("payment_method", StringType, nullable = true),
    StructField("transaction_ts", TimestampType, nullable = true),
    StructField("created_at", TimestampType, nullable = true),
    StructField("ip_address", StringType, nullable = true),
    StructField("device_id", StringType, nullable = true),
    StructField("user_agent", StringType, nullable = true)
  ))

  val merchantSchema: StructType = StructType(Seq(
    StructField("merchant_id", StringType, nullable = false),
    StructField("merchant_name", StringType, nullable = true),
    StructField("status", StringType, nullable = true),
    StructField("industry", StringType, nullable = true),
    StructField("risk_score", DecimalType(3,1), nullable = true),
    StructField("country_code", StringType, nullable = true),
    StructField("onboard_date", DateType, nullable = true),
    StructField("extra_metadata", StringType, nullable = true)
  ))

  // ------------- Data Generators -----------
  def generateTransactions(numTxns: Int):
  Seq[(
    String,               // transaction_id
      String,               // user_id
      String,               // merchant_id
      java.math.BigDecimal, // amount
      String,               // currency
      String,               // status
      String,               // reason_code
      String,               // payment_method
      java.sql.Timestamp,   // transaction_ts
      java.sql.Timestamp,   // created_at
      String,               // ip_address
      String,               // device_id
      String                // user_agent
    )] = {

    val faker      = new Faker(new Locale("en-US"))
    val statuses   = Array("COMPLETED","DECLINED","PENDING","REFUNDED","FAILED")
    val methods    = Array("CREDIT_CARD","PAYPAL","APPLE_PAY","GOOGLE_PAY")
    val reasonCodes= Array("InsufficientFunds","ExpiredCard","None","LimitExceeded")
    val rnd        = new Random()

    val startOfYear = LocalDate.now().withDayOfYear(1)
    val now         = LocalDate.now()
    val startDate   = java.util.Date.from(startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant)
    val endDate     = java.util.Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant)

    (1 to numTxns).map { _ =>
      val txnId      = "txn_" + rnd.alphanumeric.take(8).mkString
      val userId     = "U" + (1000 + rnd.nextInt(9000))
      val merchId    = "M" + (1000 + rnd.nextInt(9000))

      val randomVal  = 1.0 + rnd.nextDouble() * 499.0
      val amtBigDec  = BigDecimal(randomVal).setScale(2, BigDecimal.RoundingMode.HALF_UP).bigDecimal

      val curr       = "USD"
      val st         = statuses(rnd.nextInt(statuses.length))
      val reason     = if(st == "DECLINED" || st == "FAILED") reasonCodes(rnd.nextInt(reasonCodes.length)) else null
      val meth       = methods(rnd.nextInt(methods.length))
      val txnTs      = new java.sql.Timestamp(faker.date().between(startDate, endDate).getTime)
      val created    = new java.sql.Timestamp(System.currentTimeMillis())
      val ipAddr     = if(rnd.nextBoolean()) faker.internet().ipV4Address() else null
      val devId      = if(rnd.nextBoolean()) s"dev_${rnd.nextInt(1000)}" else null
      val agent      = if(rnd.nextBoolean()) faker.internet().userAgentAny() else null

      (txnId, userId, merchId, amtBigDec, curr, st, reason, meth, txnTs, created, ipAddr, devId, agent)
    }
  }

  def generateMerchants(numRows: Int):
  Seq[(
    String,               // merchant_id
      String,               // merchant_name
      String,               // status
      String,               // industry
      java.math.BigDecimal, // risk_score
      String,               // country_code
      java.sql.Date,        // onboard_date
      String                // extra_metadata
    )] = {

    val faker      = new Faker(new Locale("en-US"))
    val statuses   = Array("ACTIVE","INACTIVE","SUSPENDED")
    val industries = Array("Retail","Food Delivery","SaaS","Travel")
    val countries  = Array("US","GB","CA","AU")
    val rnd        = new Random()

    (1 to numRows).map { _ =>
      val mId    = "M" + (1000 + rnd.nextInt(9000))
      val mName  = faker.company().name()
      val st     = statuses(rnd.nextInt(statuses.length))
      val ind    = industries(rnd.nextInt(industries.length))

      val riskVal = rnd.nextDouble() * 10.0
      val riskBdc = BigDecimal(riskVal).setScale(1, BigDecimal.RoundingMode.HALF_UP).bigDecimal

      val cCode  = countries(rnd.nextInt(countries.length))
      val onDate = new java.sql.Date(faker.date().past(1000, java.util.concurrent.TimeUnit.DAYS).getTime)
      val extra  = if(rnd.nextBoolean()) s"salesRep=${rnd.nextInt(50)}" else null

      (mId, mName, st, ind, riskBdc, cCode, onDate, extra)
    }
  }

  // ------------- Write method -----------
  def writeUpstreamsDataToStorage(df: DataFrame, opts: WriteDestinationConfig): Unit = {
    if (opts.localPath.nonEmpty) {
      df.write.mode("overwrite").parquet(opts.localPath)
    }
    if (opts.icebergTable.nonEmpty) {
      df.writeTo(opts.icebergTable).createOrReplace()
    }
  }

  // ------------- Main entry point -----------

    def main(args: Array[String]): Unit = {
      val env = if(args.nonEmpty) args(0) else "dev"

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
    // 1) Generate data
    val numTransactions = 1000
    val numMerchants    = 500

    val txnSeq   = generateTransactions(numTransactions)
    val merchSeq = generateMerchants(numMerchants)

    val txnRDD   = spark.sparkContext.parallelize(txnSeq).map(Row.fromTuple)
    val merchRDD = spark.sparkContext.parallelize(merchSeq).map(Row.fromTuple)

    val txnDF = spark.createDataFrame(txnRDD, transactionSchema)
      .withColumn("ingested_at", current_timestamp())

    val merchDF = spark.createDataFrame(merchRDD, merchantSchema)
      .withColumn("ingested_at", current_timestamp())

      txnDF.show()
      txnDF.printSchema()

      val writeDestinationConfigTrans = WriteDestinationConfig("data/transactions","dev.transactions")
      val writeMerchants = WriteDestinationConfig("data/merchants","dev.merchants")
     // txnDF.writeTo("dev.transactions").createOrReplace()
      writeUpstreamsDataToStorage(txnDF,writeDestinationConfigTrans)
      writeUpstreamsDataToStorage(merchDF,writeMerchants)


    println(s"[INFO] Ingested $numTransactions transactions & $numMerchants merchants => local + S3 + Iceberg. env=$env")
     // println(s"[INFO] Ingested $numMerchants & $numMerchants merchants => local + S3 + Iceberg. env=$env")

    spark.stop()
  }
}