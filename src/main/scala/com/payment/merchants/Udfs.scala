package com.payment.merchants

import java.security.Timestamp

object Udfs {


  def normalizePaymentStatus(status: String): String = {

    if (status == null) null

    else {
      status.trim.toLowerCase match {
    case "completed" | "complete" => "COMPLETED"
    case "declined" => "DECLINED"
    case "failed"                 => "FAILED"
    case "pending"                => "PENDING"
    case "refunded"               => "REFUNDED"
    case other                    => other.toUpperCase
      }
    }

  }

  /** Compute a basic risk score by combining amount, merchant risk, payment method, etc. */
//  def computeRiskScore(
//                        amount: java.math.BigDecimal,
//                        merchantRisk: java.math.BigDecimal,
//                        paymentMethod: String,
//                        txnTimestamp: Timestamp
//                      ): java.math.BigDecimal = {
//    if (amount == null || merchantRisk == null) {
//      java.math.BigDecimal.ZERO
//    } else {
//      val base = merchantRisk.add(amount.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP))
//      val methodRiskBoost = paymentMethod match {
//        case "CREDIT_CARD" => 0.5
//        case "PAYPAL"      => 0.3
//        case "APPLE_PAY"   => 0.2
//        case "GOOGLE_PAY"  => 0.2
//        case _             => 0.0
//      }
//      base.add(new java.math.BigDecimal(methodRiskBoost.toString))
//    }
//  }

  /** Flag transaction as fraud if risk is high or if status+reason are suspicious. */
  def fraudDetectionFlag(
                          riskScore: java.math.BigDecimal,
                          paymentStatus: String,
                          reasonCode: String
                        ): Boolean = {
    if (riskScore == null || paymentStatus == null) {
      false
    } else {
      val highRisk = riskScore.compareTo(new java.math.BigDecimal("8.0")) > 0
      val suspiciousStatus = paymentStatus == "FAILED" || paymentStatus == "DECLINED"
      val suspiciousReason = Option(reasonCode).exists(r => r == "InsufficientFunds" || r == "ExpiredCard")
      highRisk || (suspiciousStatus && suspiciousReason)
    }
  }

  /** Parse user agent to a simple category (MOBILE, BOT, DESKTOP). */
  def parseUserAgent(userAgent: String): String = {
    if (userAgent == null) null
    else {
      val ua = userAgent.toLowerCase
      if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) "MOBILE"
      else if (ua.contains("bot")) "BOT"
      else "DESKTOP"
    }
  }

  /** Categorize transactions (MICRO, SMALL, MEDIUM, LARGE) based on amount. */
  def deriveTransactionCategory(
                                 amount: java.math.BigDecimal,
                                 paymentMethod: String
                               ): String = {
    if (amount == null) null
    else {
      val amt = amount.doubleValue()
      if (amt < 10)       "MICRO"
      else if (amt < 100) "SMALL"
      else if (amt < 500) "MEDIUM"
      else                "LARGE"
    }
  }

  /** Mask sensitive information (e.g., IP or email). */
  def maskSensitiveInfo(field: String): String = {
    if (field == null) null
    else {
      // If IP, mask last octet: "192.168.0.XXX"
      if (field.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
        val parts = field.split("\\.")
        parts.take(3).mkString(".") + ".XXX"
      }
      // If email, mask local part
      else if (field.contains("@")) {
        val Array(local, domain) = field.split("@")
        if (local.length > 1) local.head + "***@" + domain else "****@" + domain
      }
      else "MASKED"
    }
  }

}
