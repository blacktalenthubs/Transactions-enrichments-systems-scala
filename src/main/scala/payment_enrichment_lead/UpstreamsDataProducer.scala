//package payment_enrichment_lead
//
//import com.github.javafaker.Faker
//import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
//import org.joda.time.LocalDate
//
//import java.util.Locale
//import scala.util.Random
//
//object UpstreamsDataProducer {
//
//  def main(args: Array[String]): Unit = {
//
//    //////////////////////////////////////////////////////////////////////////
//    // BUILD SCOPT PARSER
//    //////////////////////////////////////////////////////////////////////////
//    val builder = OParser.builder[UpstreamConfig]
//    val parser = {
//      import builder._
//      OParser.sequence(
//        programName("UpstreamsDataProducer"),
//        head("UpstreamsDataProducer", "1.0"),
//
//        opt[Int]("numTransactions")
//          .action((x, c) => c.copy(numTransactions = x))
//          .text("Number of transactions to generate (default 1000)"),
//
//        opt[Int]("numMerchants")
//          .action((x, c) => c.copy(numMerchants = x))
//          .text("Number of merchants to generate (default 50)"),
//
//        opt[Int]("numUsers")
//          .action((x, c) => c.copy(numUsers = x))
//          .text("Number of users to generate (default 500)"),
//
//        opt[Int]("numCompliance")
//          .action((x, c) => c.copy(numComplianceRows = x))
//          .text("Number of compliance rows to generate (default 20)"),
//
//        opt[String]("transactionsTable")
//          .action((x, c) => c.copy(transactionsTable = x))
//          .text("Iceberg table for transactions (default myGlueCatalog.dev.transactions_raw)"),
//
//        opt[String]("merchantsTable")
//          .action((x, c) => c.copy(merchantsTable = x))
//          .text("Iceberg table for merchants (default myGlueCatalog.dev.merchants_raw)"),
//
//        opt[String]("usersTable")
//          .action((x, c) => c.copy(usersTable = x))
//          .text("Iceberg table for users (default myGlueCatalog.dev.users_raw)"),
//
//        opt[String]("complianceTable")
//          .action((x, c) => c.copy(complianceTable = x))
//          .text("Iceberg table for compliance rules (default myGlueCatalog.dev.compliance_raw)"),
//
//        opt[String]("localOutputPath")
//          .action((x, c) => c.copy(localOutputPath = Some(x)))
//          .text("Local path output if not using Iceberg (e.g., ./data/)"),
//
//        opt[String]("processingDate")
//          .action((x, c) => c.copy(processingDate = x))
//          .text("Date string (yyyy-MM-dd) used as partition or reference date"),
//
//        opt[Boolean]("useIceberg")
//          .action((x, c) => c.copy(useIceberg = x))
//          .text("Whether to write data to Iceberg tables. If false, writes to local parquet."),
//
//        opt[Boolean]("overwrite")
//          .action((x, c) => c.copy(overwrite = x))
//          .text("Whether to overwrite existing data."),
//
//        help("help").text("Prints usage text")
//      )
//    }
//
//    // parse CLI or show usage
//    OParser.parse(parser, args, UpstreamConfig()) match {
//      case Some(config) =>
//        run(config)
//      case _ =>
//        // arguments are bad, usage message will have been displayed
//        sys.exit(1)
//    }
//  }
//
//  //////////////////////////////////////////////////////////////////////////
//  // MAIN LOGIC
//  //////////////////////////////////////////////////////////////////////////
//
//  def run(cfg: UpstreamConfig): Unit = {
//    // Create Spark Session
//    val spark = SparkSession.builder()
//      .appName(cfg.appName)
//      .master(cfg.master)
//      // Example for iceberg usage
//      .config("spark.sql.catalog.myGlueCatalog", "org.apache.iceberg.spark.SparkCatalog")
//      .config("spark.sql.catalog.myGlueCatalog.type", "glue")
//      .config("spark.sql.catalog.myGlueCatalog.warehouse", "s3://my-iceberg-warehouse/")  // Adjust as needed
//      .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
//      .getOrCreate()
//
//    import spark.implicits._
//
//    spark.sparkContext.setLogLevel("WARN")
//
//    println(s"[INFO] Generating data with config: $cfg")
//
//    // Generate each domain
//    val merchantsDS   = generateMerchants(spark, cfg.numMerchants, cfg.processingDate)
//    val usersDS       = generateUsers(spark, cfg.numUsers, cfg.processingDate)
//    val complianceDS  = generateComplianceRules(spark, cfg.numComplianceRows, cfg.processingDate)
//    // NOTE: transactions reference merchant_ids from merchantsDS,
//    // so we pass the merchantsDS collect() into the generator
//    val merchantsList = merchantsDS.collect()  // might be large in real scenario;
//    val transactionsDS= generateTransactions(spark, cfg.numTransactions, merchantsList, cfg.processingDate)
//
//    // Write out
//    if (cfg.useIceberg) {
//      println("[INFO] Writing data to ICEBERG tables...")
//      writeToIceberg(transactionsDS.toDF(), cfg.transactionsTable, cfg.overwrite)
//      writeToIceberg(merchantsDS.toDF(),    cfg.merchantsTable,    cfg.overwrite)
//      writeToIceberg(usersDS.toDF(),        cfg.usersTable,        cfg.overwrite)
//      writeToIceberg(complianceDS.toDF(),   cfg.complianceTable,   cfg.overwrite)
//    } else {
//      // or local parquet
//      println("[INFO] Writing data to local parquet path or provided path...")
//      val path = cfg.localOutputPath.getOrElse("./local_parquet/")
//      writeToParquet(transactionsDS.toDF(), s"$path/transactions_raw", cfg.overwrite)
//      writeToParquet(merchantsDS.toDF(),    s"$path/merchants_raw",    cfg.overwrite)
//      writeToParquet(usersDS.toDF(),        s"$path/users_raw",        cfg.overwrite)
//      writeToParquet(complianceDS.toDF(),   s"$path/compliance_raw",   cfg.overwrite)
//    }
//
//    spark.stop()
//    println("[INFO] Upstreams data generation completed.")
//  }
//
//  //////////////////////////////////////////////////////////////////////////
//  // 3. DATA GENERATORS
//  //////////////////////////////////////////////////////////////////////////
//
//  /**
//   * Generate Merchants dataset using Faker.
//   * We'll produce random merchant data with a mix of statuses,
//   * some risk_score range, etc., all with partition column 'processing_date'.
//   */
//  def generateMerchants(spark: SparkSession, num: Int, procDate: String): Dataset[Merchants] = {
//    val faker = new Faker(new Locale("en-US"))
//    val rnd   = new Random()
//
//    // statuses, industries, countries for variety
//    val statuses   = Array("ACTIVE","INACTIVE","SUSPENDED")
//    val industries = Array("Retail","Food Delivery","SaaS","Travel","E-Commerce","Grocery")
//    val countries  = Array("US","GB","CA","AU")
//
//    val data = (1 to num).map { _ =>
//      val mid      = "M" + (1000 + rnd.nextInt(9000))       // e.g. M8432
//      val mName    = faker.company().name()                 // e.g. "Acme Inc."
//      val status   = statuses(rnd.nextInt(statuses.length))
//      val industry = industries(rnd.nextInt(industries.length))
//      val riskVal  = BigDecimal(rnd.nextDouble() * 10.0).setScale(2, BigDecimal.RoundingMode.HALF_UP)
//      val cc       = countries(rnd.nextInt(countries.length))
//      val onboard  = new java.sql.Date(
//        faker.date().past(1000, java.util.concurrent.TimeUnit.DAYS).getTime
//      )
//      val extra    = if (rnd.nextBoolean()) Some(s"rep-${rnd.nextInt(50)}") else None
//
//      Merchants(
//        merchant_id    = mid,
//        merchant_name  = mName,
//        status         = status,
//        industry       = industry,
//        risk_score     = riskVal,
//        country_code   = cc,
//        onboard_date   = onboard,
//        extra_metadata = extra,
//        processing_date= procDate
//      )
//    }
//
//    import spark.implicits._
//    spark.createDataset(data)(Encoders.product[Merchants])
//  }
//
//  /**
//   * Generate Users dataset.
//   */
//  def generateUsers(spark: SparkSession, num: Int, procDate: String): Dataset[Users] = {
//    val faker = new Faker(new Locale("en-US"))
//    val rnd   = new Random()
//    val countries = Array("US","GB","CA","AU","NZ")
//    val loyaltyTiers = Array("SILVER","GOLD","PLATINUM","NONE")
//
//    val data = (1 to num).map { _ =>
//      val uid   = "U" + (1000 + rnd.nextInt(9000))
//      val uname = faker.name().fullName()
//      val email = faker.internet().emailAddress()
//      val cc    = countries(rnd.nextInt(countries.length))
//      val sdate = new java.sql.Date(
//        faker.date().past(700, java.util.concurrent.TimeUnit.DAYS).getTime
//      )
//      val tier  = loyaltyTiers(rnd.nextInt(loyaltyTiers.length))
//
//      Users(
//        user_id        = uid,
//        user_name      = uname,
//        email          = email,
//        country_code   = cc,
//        signup_date    = sdate,
//        loyalty_tier   = tier,
//        processing_date= procDate
//      )
//    }
//
//    import spark.implicits._
//    spark.createDataset(data)(Encoders.product[Users])
//  }
//
//  /**
//   * Generate a small dataset of compliance rules, e.g. region restrictions.
//   */
//  def generateComplianceRules(spark: SparkSession, num: Int, procDate: String): Dataset[ComplianceRules] = {
//    val faker = new Faker(new Locale("en-US"))
//    val rnd   = new Random()
//    val regionIDs = Array("US-EAST","US-WEST","EU-CENTRAL","AP-SOUTH","LA-EAST")
//
//    val data = (1 to num).map { _ =>
//      val region = regionIDs(rnd.nextInt(regionIDs.length))
//      val ruleName  = s"RULE_${rnd.alphanumeric.take(5).mkString}"
//      val ruleValue = faker.lorem().sentence(5)
//
//      ComplianceRules(
//        region_id       = region,
//        rule_name       = ruleName,
//        rule_value      = ruleValue,
//        processing_date = procDate
//      )
//    }
//
//    import spark.implicits._
//    spark.createDataset(data)(Encoders.product[ComplianceRules])
//  }
//
//  /**
//   * Generate Transactions referencing some merchant IDs from the generated Merchants dataset.
//   */
//  def generateTransactions(
//                            spark: SparkSession,
//                            num: Int,
//                            merchants: Array[Merchants],
//                            procDate: String
//                          ): Dataset[Transactions] = {
//    val faker = new Faker(new Locale("en-US"))
//    val rnd   = new Random()
//
//    val statuses   = Array("COMPLETED","DECLINED","PENDING","REFUNDED","FAILED")
//    val methods    = Array("CREDIT_CARD","PAYPAL","APPLE_PAY","GOOGLE_PAY")
//    val reasons    = Array("InsufficientFunds","ExpiredCard","None","LimitExceeded")
//
//    // Use current year for transaction range
//    val startOfYear = LocalDate.now().withDayOfYear(1)
//    val now         = LocalDate.now()
//    val startDate   = java.util.Date.from(startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant)
//    val endDate     = java.util.Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant)
//
//    // Extract merchant IDs for references
//    val merchantIds = merchants.map(_.merchant_id)
//    val merchantSize= merchantIds.length
//
//    val data = (1 to num).map { _ =>
//      val txid   = "txn_" + rnd.alphanumeric.take(8).mkString
//      val userid = "U" + (1000 + rnd.nextInt(9000))
//      val merchId=
//        if(merchantSize > 0) merchantIds(rnd.nextInt(merchantSize))
//        else "M9999" // fallback if no merchants
//
//      // amounts from 1.0 -> 999.99
//      val randAmt = 1.0 + rnd.nextDouble() * 999.0
//      val amt     = BigDecimal(randAmt).setScale(2, BigDecimal.RoundingMode.HALF_UP)
//
//      val curr    = "USD"
//      val st      = statuses(rnd.nextInt(statuses.length))
//      val reason  = if(st == "DECLINED" || st == "FAILED"){
//        Some(reasons(rnd.nextInt(reasons.length)))
//      } else None
//      val pm      = methods(rnd.nextInt(methods.length))
//      val txnTs   = new Timestamp(
//        faker.date().between(startDate, endDate).getTime
//      )
//      val created = new Timestamp(System.currentTimeMillis())
//      val ip      = if(rnd.nextBoolean()) Some(faker.internet().ipV4Address()) else None
//      val devId   = if(rnd.nextBoolean()) Some(s"dev_${rnd.nextInt(1000)}") else None
//      val agent   = if(rnd.nextBoolean()) Some(faker.internet().userAgentAny()) else None
//
//      Transactions(
//        transaction_id = txid,
//        user_id        = userid,
//        merchant_id    = merchId,
//        amount         = amt,
//        currency       = curr,
//        status         = st,
//        reason_code    = reason,
//        payment_method = pm,
//        transaction_ts = txnTs,
//        created_at     = created,
//        ip_address     = ip,
//        device_id      = devId,
//        user_agent     = agent,
//        processing_date= procDate
//      )
//    }
//
//    import spark.implicits._
//    spark.createDataset(data)(Encoders.product[Transactions])
//  }
//
//  //////////////////////////////////////////////////////////////////////////
//  // 4. WRITE METHODS (ICEBERG OR PARQUET)
//  //////////////////////////////////////////////////////////////////////////
//
//  /**
//   * Writes to an Iceberg table. If partitioning is desired,
//   * configure the table's partition spec in Iceberg itself or do partial code here.
//   * For demonstration, we do a simple append or overwrite.
//   */
//  def writeToIceberg(df: DataFrame, tableName: String, overwrite: Boolean): Unit = {
//    if(overwrite){
//      println(s"[INFO] Overwriting data in Iceberg table: $tableName")
//      df.writeTo(tableName).overwritePartitions()
//      // or createOrReplace() if new table
//      // but typically you'd have the table created with partition spec in advance
//    } else {
//      println(s"[INFO] Appending data to Iceberg table: $tableName")
//      df.writeTo(tableName).append()
//    }
//  }
//
//  /**
//   * Writes local or custom path to Parquet. We'll demonstrate simple 'overwrite'
//   * or 'append' modes.
//   */
//  def writeToParquet(df: DataFrame, path: String, overwrite: Boolean): Unit = {
//    val writer = if(overwrite) df.write.mode("overwrite") else df.write.mode("append")
//    println(s"[INFO] Writing parquet to path: $path , overwrite=$overwrite")
//    writer.parquet(path)
//  }
//
//}