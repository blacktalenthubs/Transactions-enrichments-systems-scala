package com.payment.merchants

import com.github.javafaker.Faker
import org.apache.spark.sql.catalyst.dsl.expressions.StringToAttributeConversionHelper
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.util.Locale
import java.time.{LocalDate, ZoneId}
import org.apache.spark.sql.functions._

import scala.util.Random


object UpstreamsDataProducer {


  private val knownMerchantNames = Array(
    "Amazon", "Target", "Walmart", "Best Buy", "Costco", "Ikea",
    "Home Depot", "Starbucks", "McDonald's", "KFC", "Uber Eats",
    "Netflix", "Apple Online", "Nike", "Adidas", "Zara",
    "Southwest Airlines", "Airbnb", "Delta Airlines",
    "Booking.com", "Etsy", "Sephora", "Whole Foods"
  )

  /**
   * Generate Merchants with a mix of known brand names and random companies.
   * Some duplicates are allowed to simulate real large brands repeatedly.
   */
  def generateMerchants(numRows: Int): Seq[Merchant] = {
    val faker     = new Faker(new Locale("en-US"))
    val rnd       = new Random()
    val statuses  = Array("ACTIVE","INACTIVE","SUSPENDED")
    val industries= Array("Retail","Food Delivery","SaaS","Travel","E-Commerce","Grocery")
    val countries = Array("US","GB","CA","AU")

    (1 to numRows).map { idx =>
      // ~50% chance to pick from knownMerchantNames, otherwise use a random company
      val useKnownBrand = rnd.nextBoolean()
      val brandName =
        if (useKnownBrand)
          knownMerchantNames(rnd.nextInt(knownMerchantNames.length))
        else
          faker.company().name()

      val mid      = "M" + (1000 + rnd.nextInt(9000))
      val mName    = s"$brandName" // occasionally add index, etc.
      val status   = statuses(rnd.nextInt(statuses.length))
      val industry = industries(rnd.nextInt(industries.length))
      val riskVal  = rnd.nextDouble()*10.0
      val risk     = BigDecimal(riskVal).setScale(1, BigDecimal.RoundingMode.HALF_UP)
      val country  = countries(rnd.nextInt(countries.length))
      val onboard  = new java.sql.Date(faker.date().past(1000, java.util.concurrent.TimeUnit.DAYS).getTime)
      val extra    = if (rnd.nextBoolean()) s"salesRep=${rnd.nextInt(50)}" else null

      Merchant(
        merchant_id  = mid,
        merchant_name= mName,
        status       = status,
        industry     = industry,
        risk_score   = risk,
        country_code = country,
        onboard_date = onboard,
        extra_metadata = extra
      )
    }
  }

  // -----------------------------------------------------------------
  // 2) Generate Users (unchanged, but can add more realism)
  // -----------------------------------------------------------------
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
      UserRecord(uid, name, email, phone, cc, cdt)
    }
  }

  // -----------------------------------------------------------------
  // 3) Generate Transactions with Weighted Merchant Assignments
  // -----------------------------------------------------------------
  def generateTransactions(numberOfTransactions: Int, existingMerchants: Seq[Merchant]): Seq[Transactions] = {
    val faker          = new Faker(new Locale("en-US"))
    val rnd            = new Random()
    val paymentStatus  = Array("COMPLETED", "DECLINED", "PENDING", "REFUNDED", "FAILED")
    val methods        = Array("CREDIT_CARD", "PAYPAL", "APPLE_PAY", "GOOGLE_PAY")
    val failureReasons = Array("InsufficientFunds", "ExpiredCard", "None", "LimitExceeded")
    val merchantsMap   = existingMerchants.map(_.merchant_id)

    // For realistic date ranges:
    val startOfYear = LocalDate.now().withDayOfYear(1)
    val now         = LocalDate.now()
    val startDate   = java.util.Date.from(startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant)
    val endDate     = java.util.Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant)

    (1 to numberOfTransactions).map { _ =>
      val txId    = "txn_" + rnd.alphanumeric.take(8).mkString
      val userId  = "U" + (1000 + rnd.nextInt(9000))

      // Weighted approach: top 10% merchants get 50% of transactions
      // so we see certain “big brand” merchants more frequently
      val chosenMerchant =
        if (rnd.nextDouble() < 0.5 && existingMerchants.nonEmpty) {
          // pick from top 10 merchants
          existingMerchants.take( (existingMerchants.size*0.2).toInt max 1 )
            .lift(rnd.nextInt( (existingMerchants.size*0.2).toInt max 1))
            .map(_.merchant_id)
            .getOrElse(merchantsMap(rnd.nextInt(merchantsMap.size)))
        } else {
          // otherwise pick from entire set
          merchantsMap(rnd.nextInt(merchantsMap.size))
        }

      // Amount from 1.0 -> 999.99
      val randAmt = 1.0 + rnd.nextDouble()*999.0
      val amt     = BigDecimal(randAmt).setScale(2, BigDecimal.RoundingMode.HALF_UP)

      val curr    = "USD"
      val status  = paymentStatus(rnd.nextInt(paymentStatus.length))
      val reasonCd= if (status == "DECLINED" || status == "FAILED")
        failureReasons(rnd.nextInt(failureReasons.length))
      else
        null
      val pm      = methods(rnd.nextInt(methods.length))
      val txnTs   = new java.sql.Timestamp(faker.date().between(startDate, endDate).getTime)
      val created = new java.sql.Timestamp(System.currentTimeMillis())
      val ip      = if (rnd.nextBoolean()) faker.internet().ipV4Address() else null
      val devId   = if (rnd.nextBoolean()) s"dev_${rnd.nextInt(1000)}" else null
      val agent   = if (rnd.nextBoolean()) faker.internet().userAgentAny() else null

      Transactions(
        txId,
        userId,
        chosenMerchant,
        amt,
        curr,
        status,
        reasonCd,
        pm,
        txnTs,
        created,
        ip,
        devId,
        agent
      )
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
    val merchantCount = 80
   // val merchants     = generateMerchants(merchantCount)

   val users =  generateUsers(100)
    val merchants = generateMerchants(merchantCount)
    val transactions =generateTransactions(50,merchants)
    val locations = generateLocations(50)

    val transWrite = DatasetUtils.WriteDestinationConfig("data/transactions","dev.transactions")
    val usersWrite = DatasetUtils.WriteDestinationConfig("data/users","dev.users")
    val merchantWrite = DatasetUtils.WriteDestinationConfig("data/merchants","dev.merchants")
    val locationWrite = DatasetUtils.WriteDestinationConfig("data/locations","dev.locations")

    import spark.implicits._
    val txnDF   = spark.createDataset(transactions).withColumn("ingested_at", current_timestamp())

    txnDF.show()
    val merchDF = spark.createDataset(merchants).withColumn("ingested_at", current_timestamp())
    val userDF  = spark.createDataset(users).withColumn("ingested_at", current_timestamp())
    val locDF   = spark.createDataset(locations).withColumn("ingested_at", current_timestamp())

    txnDF.show()

    DatasetUtils.writeDataToStorage(txnDF,transWrite)
    DatasetUtils.writeDataToStorage(merchDF,merchantWrite)
    DatasetUtils.writeDataToStorage(userDF,usersWrite)
    DatasetUtils.writeDataToStorage(locDF,locationWrite)

  }


  def generateSkewedTransactions(spark: SparkSession, numRows: Int): DataFrame = {
    import spark.implicits._

    val categories = Array("Books", "Grocery", "Electronics", "Clothing", "Shoes")
    val rnd = new Random()

    val data = (1 to numRows).map { _ =>
      val isSkew = rnd.nextDouble() < 0.70 // 70% chance we pick the "hot" user
      val userId = if (isSkew) "User_SK" else "User_" + (1000 + rnd.nextInt(9000))
      val amount = rnd.nextDouble() * 500
      val cat    = categories(rnd.nextInt(categories.length))
      (userId, amount, cat)
    }
  spark.createDataFrame(data).toDF("userId", "amount", "category")
  }


}
