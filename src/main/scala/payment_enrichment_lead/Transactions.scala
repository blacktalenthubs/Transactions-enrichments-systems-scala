//package payment_enrichment_lead
//
//import org.joda.time.LocalDate
//
//import java.sql.Timestamp
//
///**
// * PHASE 1: Upstreams Data Producers
// *
// * Goal: Generate synthetic data to mimic real-world sources for transactions,
// * merchants, users, and compliance references. We'll write these to Iceberg
// * tables or local Parquet, partitioned by processing_date (optional).
// *
// * The code below shows:
// * 1) Case class definitions for each domain (Transactions, Merchants, Users, ComplianceRules).
// * 2) Integration with JavaFaker to produce realistic fields.
// * 3) Writing data out to Iceberg or Parquet.
// * 4) A standalone Spark job that can be scheduled or run on-demand.
// *
// * NOTE: For truly "production grade," adapt to your logging, config management,
// * credentials, error handling, and actual environment constraints (EMR, Yarn, etc.).
// */
//
////////////////////////////////////////////////////////////////////////////////////
//// 1. CASE CLASS DEFINITIONS
////////////////////////////////////////////////////////////////////////////////////
//
//case class Transactions(
//                         transaction_id: String,
//                         user_id: String,
//                         merchant_id: String,
//                         amount: BigDecimal,
//                         currency: String,
//                         status: String,
//                         reason_code: Option[String],
//                         payment_method: String,
//                         transaction_ts: Timestamp,
//                         created_at: Timestamp,
//                         ip_address: Option[String],
//                         device_id: Option[String],
//                         user_agent: Option[String],
//                         processing_date: String     // We'll store "yyyy-MM-dd" for partitioning, if desired
//                       )
//
//case class Merchants(
//                      merchant_id: String,
//                      merchant_name: String,
//                      status: String,
//                      industry: String,
//                      risk_score: BigDecimal,
//                      country_code: String,
//                      onboard_date: java.sql.Date,
//                      extra_metadata: Option[String],
//                      processing_date: String
//                    )
//
//case class Users(
//                  user_id: String,
//                  user_name: String,
//                  email: String,
//                  country_code: String,
//                  signup_date: java.sql.Date,
//                  loyalty_tier: String,
//                  processing_date: String
//                )
//
///**
//
// */
//case class ComplianceRules(
//                            region_id: String,
//                            rule_name: String,
//                            rule_value: String,
//                            processing_date: String
//                          )
//
////////////////////////////////////////////////////////////////////////////////////
//// 2. COMMAND-LINE ARGUMENT PARSING (SCOPT)
////////////////////////////////////////////////////////////////////////////////////
//
///**
// * Defines our CLI arguments for controlling how much data to generate,
// * which environment/catalog to use, etc.
// */
//case class UpstreamConfig(
//                           // Basic parameters for controlling data volumes
//                           numTransactions: Int        = 1000,
//                           numMerchants: Int           = 50,
//                           numUsers: Int               = 500,
//                           numComplianceRows: Int      = 20,
//
//                           // Where to write: e.g., "myGlueCatalog.dev.transactions_raw" or local path
//                           transactionsTable: String   = "myGlueCatalog.dev.transactions_raw",
//                           merchantsTable: String      = "myGlueCatalog.dev.merchants_raw",
//                           usersTable: String          = "myGlueCatalog.dev.users_raw",
//                           complianceTable: String     = "myGlueCatalog.dev.compliance_raw",
//
//                           // Could also support local paths if not writing to Iceberg
//                           localOutputPath: Option[String] = None,
//
//                           // Additional Spark or partition date
//                           processingDate: String      = LocalDate.now().toString,  // default to today
//
//                           // Spark parameters
//                           master: String              = "local[*]",
//                           appName: String             = "UpstreamsDataProducer",
//                           useIceberg: Boolean         = true,   // whether to write to Iceberg or just local parquet
//                           overwrite: Boolean          = false   // whether to overwrite existing data
//                         )
//case class PipelineParams(
//                           env: String = "dev",
//                           configFile: String = "",  // path to application-dev.yml
//                           timestamp: String = "",
//                           sparkLogLevel: String = "WARN",
//                           closeSession: Boolean = true,
//                           debug: Boolean = false
//                         )
//
//case class SparkConfigEntry(key: String, value: String)
//
//case class PipelineConfig(
//                           environment: String,
//                           sparkSettings: Map[String, String],
//                           tableNames: Map[String, String],
//                           paths: Map[String, String],
//                           partitions: Map[String, String]
//                         )