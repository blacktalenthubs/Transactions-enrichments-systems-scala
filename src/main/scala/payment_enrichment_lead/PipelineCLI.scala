//package payment_enrichment_lead
//
//import scopt.{OParser, OParserBuilder}
//
//object PipelineCLI {
//
//  def parse(args: Array[String]): Option[PipelineParams] = {
//    val builder: OParserBuilder[PipelineParams] = OParser.builder[PipelineParams]
//
//    val parser = {
//      import builder._
//      OParser.sequence(
//        programName("PaymentDataPipeline"),
//        head("PaymentDataPipeline", "1.0"),
//
//        opt[String]("env")
//          .action((x, c) => c.copy(env = x))
//          .text("Environment: dev, staging, prod (default: dev)"),
//
//        opt[String]("configFile")
//          .required()
//          .action((x, c) => c.copy(configFile = x))
//          .text("Path to the YAML config file (e.g., application-dev.yml)"),
//
//        opt[String]("timestamp")
//          .action((x, c) => c.copy(timestamp = x))
//          .text("Timestamp or partition date for this run"),
//
//        opt[String]("sparkLogLevel")
//          .action((x, c) => c.copy(sparkLogLevel = x))
//          .text("Spark log level (default: WARN)"),
//
//        opt[Boolean]("closeSession")
//          .action((x, c) => c.copy(closeSession = x))
//          .text("Whether to close Spark session at the end (default: true)"),
//
//        opt[Unit]("debug")
//          .action((_, c) => c.copy(debug = true))
//          .text("Enable debug mode"),
//
//        help("help")
//          .text("Prints usage text")
//      )
//    }
//
//    OParser.parse(parser, args, PipelineParams()) match {
//      case Some(config) => Some(config)
//      case None         => None
//    }
//  }
//}