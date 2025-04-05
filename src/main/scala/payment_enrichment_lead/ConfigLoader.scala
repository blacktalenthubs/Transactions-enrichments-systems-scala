//package payment_enrichment_lead
//
//import org.yaml.snakeyaml.Yaml
//
//import java.io.FileInputStream
//import scala.collection.JavaConverters.mapAsScalaMapConverter
//
//object ConfigLoader {
//
//  def loadAndMerge(
//                    cliParams: PipelineParams
//                  ): PipelineConfig = {
//
//    // 1) Load YAML from file
//    val yaml = new Yaml()
//    val inputStream = new FileInputStream(cliParams.configFile)
//    val data = yaml.loadAs(inputStream, classOf[java.util.Map[String, Object]])
//    inputStream.close()
//
//    val rootMap = data.asScala.toMap // convert from Java to Scala map
//
//    // 2) Extract environment from YAML (or use cliParams.env)
//    val environment = rootMap.get("environment") match {
//      case Some(s: String) => s
//      case _               => cliParams.env
//    }
//
//    // 3) Parse Spark config
//    //    We expect "spark" -> { "spark.sql.shuffle.partitions" -> 200, ... }
//    val sparkMap = rootMap.get("spark") match {
//      case Some(m: java.util.Map[_, _]) =>
//        m.asScala.toMap.map { case (k: String, v: Any) => (k, v.toString) }
//      case _ => Map.empty[String, String]
//    }
//
//    // 4) Parse table names
//    val tablesMap = rootMap.get("tables") match {
//      case Some(m: java.util.Map[_, _]) =>
//        m.asScala.toMap.map { case (k: String, v: Any) => (k, v.toString) }
//      case _ => Map.empty[String, String]
//    }
//
//    // 5) Parse paths
//    val pathsMap = rootMap.get("paths") match {
//      case Some(m: java.util.Map[_, _]) =>
//        m.asScala.toMap.map { case (k: String, v: Any) => (k, v.toString) }
//      case _ => Map.empty[String, String]
//    }
//
//    // 6) Parse partitions
//    val partsMap = rootMap.get("partitions") match {
//      case Some(m: java.util.Map[_, _]) =>
//        m.asScala.toMap.map { case (k: String, v: Any) => (k, v.toString) }
//      case _ => Map.empty[String, String]
//    }
//
//    // 7) Build final PipelineConfig
//    val finalConfig = PipelineConfig(
//      environment = environment,
//      sparkSettings = sparkMap,
//      tableNames = tablesMap,
//      paths = pathsMap,
//      partitions = partsMap
//    )
//
//    // For example, override environment if user provided a different --env:
//    val mergedEnv = if (cliParams.env.nonEmpty) cliParams.env else finalConfig.environment
//    finalConfig.copy(environment = mergedEnv)
//  }
//
//}