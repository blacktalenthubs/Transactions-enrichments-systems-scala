package com.payment.merchants

import com.github.javafaker.Faker
import org.apache.spark.sql.SparkSession

import java.util.Locale
import java.time.{LocalDate, ZoneId}
import org.apache.spark.sql.functions._

import scala.util.Random


object UpstreamsDataProducer {

  def generateTransactions(numberOfTransactions: Int): Seq[Transactions] = {

    val faker = new Faker(new Locale("en-US"))
    val rnd = new Random()
    val paymentStatus = Array("COMPLETED", "DECLINED", "PENDING", "REFUNDED", "FAILED")

    val methods = Array("CREDIT_CARD", "PAYPAL", "APPLE_PAY", "GOOGLE_PAY")
    val reasons = Array("InsufficientFunds", "ExpiredCard", "None", "LimitExceeded")


    val startOfYear = LocalDate.now().withDayOfYear(1)
    val endOfYear = LocalDate.now()
    val startDate = java.util.Date.from(startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant)
    val endDate = java.util.Date.from(endOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant)

    (1 to numberOfTransactions).map { _ =>
      val txId = "txn_" + rnd.alphanumeric.take(8).mkString
      val userId = "U" + (1000 + rnd.nextInt(9000))
      val merch = "M" + (1000 + rnd.nextInt(9000))

      val randAmt = 1.0 + rnd.nextDouble() * 499.0
      val amt = BigDecimal(randAmt).setScale(2, BigDecimal.RoundingMode.HALF_UP)
      val curr = "USD"
      val st = paymentStatus(rnd.nextInt(paymentStatus.length))
      val reasonCd = if (st == "DECLINED" || st == "FAILED") reasons(rnd.nextInt(reasons.length)) else null
      val pm = methods(rnd.nextInt(methods.length))
      val txnTs = new java.sql.Timestamp(faker.date().between(startDate, endDate).getTime)
      val created = new java.sql.Timestamp(System.currentTimeMillis())
      val ip = if (rnd.nextBoolean()) faker.internet().ipV4Address() else null
      val devId = if (rnd.nextBoolean()) s"dev_${rnd.nextInt(1000)}" else null
      val agent = if (rnd.nextBoolean()) faker.internet().userAgentAny() else null

      Transactions(txId, userId, merch, amt, curr, st, reasonCd, pm, txnTs, created, ip, devId, agent)

    }

  }

  /** Generate User data. */
  def generateUsers(numRows: Int): Seq[UserRecord] = {
    val faker     = new Faker(new Locale("en-US"))
    val rnd       = new Random()
    val countries = Array("US","GB","CA","AU","NZ")

    (1 to numRows).map { _ =>
      val uid  = "U" + (1000 + rnd.nextInt(9000))
      val name = faker.name().fullName()
      val email= faker.internet().emailAddress()
      val phone= faker.phoneNumber().phoneNumber()
      val cc   = countries(rnd.nextInt(countries.length))
      val cdt  = new java.sql.Date(faker.date().past(700, java.util.concurrent.TimeUnit.DAYS).getTime)

      UserRecord(uid,name,email,phone,cc,cdt)
    }
  }

  /** Generate Merchant data. */
  def generateMerchants(numRows: Int): Seq[Merchant] = {
    val faker     = new Faker(new Locale("en-US"))
    val rnd       = new Random()
    val statuses  = Array("ACTIVE","INACTIVE","SUSPENDED")
    val industries= Array("Retail","Food Delivery","SaaS","Travel")
    val countries = Array("US","GB","CA","AU")

    (1 to numRows).map { _ =>
      val mid = "M" + (1000 + rnd.nextInt(9000))
      val mname = faker.company().name()
      val st    = statuses(rnd.nextInt(statuses.length))
      val ind   = industries(rnd.nextInt(industries.length))
      val riskVal = rnd.nextDouble()*10.0
      val risk  = BigDecimal(riskVal).setScale(1, BigDecimal.RoundingMode.HALF_UP)
      val cc    = countries(rnd.nextInt(countries.length))
      val onboard= new java.sql.Date(faker.date().past(1000, java.util.concurrent.TimeUnit.DAYS).getTime)
      val extra = if(rnd.nextBoolean()) s"salesRep=${rnd.nextInt(50)}" else null

      Merchant(mid,mname,st,ind,risk,cc,onboard,extra)
    }
  }

  /** Generate Location data. */
  def generateLocations(numRows: Int): Seq[Location] = {
    val faker = new Faker(new Locale("en-US"))
    val rnd   = new Random()
    val regions = Array("North","South","East","West")

    (1 to numRows).map { _ =>
      val lid   = "L" + (1000 + rnd.nextInt(9000))
      val cntry = faker.address().countryCode()
      val reg   = regions(rnd.nextInt(regions.length))
      val city  = faker.address().cityName()
      val postal= faker.address().zipCode()

      Location(lid, cntry, reg, city, postal)
    }
  }


  def main(args: Array[String]):Unit= {


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

   val users =  UpstreamsDataProducer.generateUsers(100)
    val merchants = UpstreamsDataProducer.generateMerchants(50)
    val transactions = UpstreamsDataProducer.generateTransactions(1000)
    val locations = UpstreamsDataProducer.generateLocations(1000)

    val transWrite = DatasetUtils.WriteDestinationConfig("data/transactions","dev.transactions")
    val usersWrite = DatasetUtils.WriteDestinationConfig("data/transactions","dev.users")
    val merchantWrite = DatasetUtils.WriteDestinationConfig("data/transactions","dev.merchants")
    val locationWrite = DatasetUtils.WriteDestinationConfig("data/transactions","dev.locations")

    import spark.implicits._
    val txnDF   = spark.createDataset(users).withColumn("ingested_at", current_timestamp())
    val merchDF = spark.createDataset(merchants).withColumn("ingested_at", current_timestamp())
    val userDF  = spark.createDataset(transactions).withColumn("ingested_at", current_timestamp())
    val locDF   = spark.createDataset(locations).withColumn("ingested_at", current_timestamp())

    DatasetUtils.writeDataToStorage(txnDF,transWrite)
    DatasetUtils.writeDataToStorage(merchDF,merchantWrite)
    DatasetUtils.writeDataToStorage(userDF,usersWrite)
    DatasetUtils.writeDataToStorage(locDF,locationWrite)


  }
}
