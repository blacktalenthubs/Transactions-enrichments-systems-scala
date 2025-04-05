//package payment_enrichment_lead
//
//
//import org.apache.spark.sql.{SparkSession, DataFrame}
//import com.payment.config.{PipelineCLI, PipelineParams, ConfigLoader, PipelineConfig}
//import org.apache.logging.log4j.{LogManager, Logger}
//
//object MainApp {
//
//  private val logger: Logger = LogManager.getLogger(getClass)
//
//  def main(args: Array[String]): Unit = {
//    // 1) Parse CLI
//    val maybeParams: Option[PipelineParams] = PipelineCLI.parse(args)
//    if (maybeParams.isEmpty) {
//      sys.exit(1) // scopt already printed error/help
//    }
//    val cliParams = maybeParams.get
//
//    // 2) Load config from YAML + CLI merges
//    val pipelineConfig: PipelineConfig = ConfigLoader.loadAndMerge(cliParams)
//
//    // 3) Initialize Spark
//    val spark = buildSparkSession(pipelineConfig, cliParams)
//    spark.sparkContext.setLogLevel(cliParams.sparkLogLevel)
//
//    logger.info(s"Loaded config: $pipelineConfig")
//
//    // 4) Example: use config values for reading/writing
//    val transactionTable = pipelineConfig.tableNames.getOrElse("transactions_raw", "dev.transactions_raw_fallback")
//    logger.info(s"Transaction table is set to: $transactionTable")
//
//    // For demonstration, you might read a table:
//    // val df = spark.read.table(transactionTable)
//    // do something with df
//
//    // 5) ...
//    // end job
//
//    if (cliParams.closeSession) spark.close()
//  }
//
//  private def buildSparkSession(cfg: PipelineConfig, params: PipelineParams): SparkSession = {
//    val builder = SparkSession.builder().appName("PaymentDataPipeline")
//
//    // set spark config from pipelineConfig
//    cfg.sparkSettings.foreach { case (key, value) =>
//      builder.config(key, value)
//    }
//
//    // if you want to set more config from CLI, do so:
//    // builder.config("spark.dynamicAllocation.enabled", "true")
//
//    builder.getOrCreate()
//  }
//}