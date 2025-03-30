package com.payment.merchants

object EnrichmentEngineeSparkContext {

  // 1) Optional: A UDF or direct Scala function to classify user agents
  // We'll define it as a Scala function and register as a Spark UDF for DataFrame usage.
  def parseUserAgent(ua: String): String = {
    if (ua == null) null
    else {
      val lower = ua.toLowerCase
      if (lower.contains("mobile") || lower.contains("iphone") || lower.contains("android")) "MOBILE"
      else if (lower.contains("bot")) "BOT"
      else "DESKTOP"
    }
  }

}
