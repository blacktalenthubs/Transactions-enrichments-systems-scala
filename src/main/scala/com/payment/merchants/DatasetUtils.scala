package com.payment.merchants

import org.apache.spark.sql.{DataFrame, SparkSession}

object DatasetUtils {
  case class WriteDestinationConfig(
                                     localPath:    String  = "",
                                     icebergTable: String  = ""
                                   )



  def getSparkSetup:SparkSession = {
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

    spark
  }


  def writeDataToStorage(df:DataFrame,opts:WriteDestinationConfig) = {

    if(!df.isEmpty && opts.icebergTable.nonEmpty){
      df.writeTo(opts.icebergTable).createOrReplace()
    }
    if(!df.isEmpty && opts.localPath.nonEmpty){
      df.write.mode("overwrite").parquet(opts.localPath)
    }
  }
}
