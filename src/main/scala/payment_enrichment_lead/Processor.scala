package payment_enrichment_lead

import org.apache.spark.sql.{DataFrame, SparkSession}

trait Processor {
  def run(): DataFrame
}