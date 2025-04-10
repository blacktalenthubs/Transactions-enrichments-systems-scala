package payments

import com.payment.merchants.DatasetUtils
import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.nio.file.Files

class UpstreamsDataProducerTests extends AnyFunSuite with Matchers with BeforeAndAfterAll{

  lazy val spark:SparkSession = SparkSession.builder()
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


  override def afterAll():Unit = {
    spark.stop()
  }


  test("Spark session be properly configured"){
    spark should not be (null)
  }

  test("writing a non-empty dataframe to a local path should create parquet files"){
    val tempDir = Files.createTempDirectory("test_local_write").toFile.getAbsolutePath
    import spark.implicits._
    val data = Seq((1,"credit",10.00),(2,"debit",20.00))

    val finalDF = data.toDF("id","type","amount")

    val path = DatasetUtils.WriteDestinationConfig(tempDir,"")
    DatasetUtils.writeDataToStorage(finalDF,path)
    Files.list(java.nio.file.Paths.get(tempDir)).toArray.length should be > 0

  }

  test("Writing to the same local destination should overwrite previous data") {
    val tempDir = Files.createTempDirectory("test_overwrite").toFile.getAbsolutePath
    import spark.implicits._
    // Write initial DataFrame
    val df1 = Seq((1, "first")).toDF("id", "value")
    val opts = DatasetUtils.WriteDestinationConfig(localPath = tempDir, icebergTable = "")
    DatasetUtils.writeDataToStorage(df1, opts)
    val count1 = spark.read.parquet(tempDir).count()

    // Overwrite with a new DataFrame
    val df2 = Seq((2, "second"), (3, "third")).toDF("id", "value")
    DatasetUtils.writeDataToStorage(df2, opts)
    val count2 = spark.read.parquet(tempDir).count()

    count2 shouldEqual 2
    count1 should not equal count2
  }
  test("No write operation should occur when no destination is provided") {
    import spark.implicits._
    val df = Seq((1, "data")).toDF("id", "value")
    // Both localPath and icebergTable are empty.
    val opts = DatasetUtils.WriteDestinationConfig(localPath = "", icebergTable = "")
    noException should be thrownBy DatasetUtils.writeDataToStorage(df, opts)
  }

  test("Spark SQL query execution should return the expected result") {
    import spark.implicits._
    val df = Seq((1, "a"), (2, "b")).toDF("id", "letter")
    df.createOrReplaceTempView("test_table")
    val result = spark.sql("SELECT id FROM test_table WHERE letter = 'a'")
    val ids = result.collect().map(_.getInt(0))
    ids shouldEqual Array(1)
  }

  test("Spark SQL query execution should return the expected result 2") {
    import spark.implicits._
    val df = Seq((1, "a"), (2, "b")).toDF("id", "letter")
    df.createOrReplaceTempView("test_table")
    val result = spark.sql("SELECT id FROM test_table WHERE letter = 'a'")
    val ids = result.collect().map(_.getInt(0))
    ids shouldEqual Array(1)
  }

}
