package com.payment.merchants

import com.github.javafaker.Faker
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

object DebugShowNamespaces {

  def main(args: Array[String]): Unit = {

      // 1) Build a SparkSession pointing defaultCatalog to myGlueCatalog
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

      // 2) Print all namespaces from defaultCatalog = myGlueCatalog
      println("\n=== SHOW NAMESPACES ===")
      spark.sql("SHOW NAMESPACES").show(false)

      // 3) Create the 'dev' namespace if missing
      spark.sql("CREATE NAMESPACE IF NOT EXISTS dev")

      // 4) Generate a tiny DataFrame with 5 rows using Faker
      val faker = new Faker()
      val sampleData = (1 to 5).map { i =>
        val id = i
        val name = faker.name().fullName()
        val city = faker.address().cityName()
        (id, name, city)
      }

      val schema = StructType(Seq(
        StructField("id", IntegerType, nullable = false),
        StructField("name", StringType, nullable = true),
        StructField("city", StringType, nullable = true)
      ))

      import spark.implicits._
      val df = spark.createDataFrame(
        spark.sparkContext.parallelize(sampleData.map(Row.fromTuple)),
        schema
      )

      df.show(false)
      df.printSchema()

      // 5) Write to dev.testtable using the Iceberg v2 writer syntax (no .mode("overwrite"))
    df.writeTo("dev.testtable").createOrReplace()

      // Alternatively, you can do:
      // df.writeTo("dev.testtable").create()
      // if you only want to create it if it doesn't exist

//      // 6) Verify table was created
//      println("\n=== SHOW TABLES IN dev ===")
//      spark.sql("SHOW TABLES IN dev").show(false)
//
//      // 7) Read back
//      val readDF = spark.read.table("dev.testtable")
     // readDF.show(false)
      spark.stop()
    }
}
