import com.payment.merchants.DatasetUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.apache.spark.sql.SparkSession

class UpstreamsDataProducerTests extends AnyFunSuite with Matchers with BeforeAndAfterAll{

  lazy val spark:SparkSession = DatasetUtils.getSparkSetup

  @Test



}
