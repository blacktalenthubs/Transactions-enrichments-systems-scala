package com.payment.merchants

case class Transactions(
                         transaction_id: String,
                         user_id: String,
                         merchant_id: String,
                         amount: BigDecimal,
                         currency: String,
                         status: String,
                         reason_code: String,
                         payment_method: String,
                         transaction_ts: java.sql.Timestamp,
                         created_at: java.sql.Timestamp,
                         ip_address: String,
                         device_id: String,
                         user_agent: String
                       )

case class Merchant(
                     merchant_id: String,
                     merchant_name: String,
                     status: String,
                     industry: String,
                     risk_score: BigDecimal,
                     country_code: String,
                     onboard_date: java.sql.Date,
                     extra_metadata: String
                   )

case class UserRecord(
                       user_id: String,
                       user_name: String,
                       email: String,
                       phone: String,
                       country: String,
                       created_at: java.sql.Date
                     )

case class Location(
                     location_id: String,
                     country: String,
                     region: String,
                     city: String,
                     postal_code: String
                   )

case class MetricsRecord(datasetName: String, recordCount: Long, totalAmount: Option[Double], processingTimestamp: java.sql.Timestamp)
