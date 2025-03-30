package payment_enrichment_lead

import java.sql.Timestamp

object UpstreamsDataCurations {

  /**
   *
   * @param transaction_id
   * @param user_id
   * @param merchant_id
   * @param amount
   * @param currency
   * @param status
   * @param reason_code
   * @param payment_method
   * @param transaction_ts
   * @param created_at
   * @param ip_address
   * @param device_id
   * @param user_agent
   * @param processing_date
   */
  case class Transactions( transaction_id: String, user_id: String, merchant_id: String, amount: BigDecimal, currency: String, status: String, reason_code: Option[String], payment_method: String, transaction_ts: Timestamp, created_at: Timestamp, ip_address: Option[String], device_id: Option[String], user_agent: Option[String], processing_date: String)


  /**
   *
   * @param merchant_id
   * @param merchant_name
   * @param status
   * @param industry
   * @param risk_score
   * @param country_code
   * @param onboard_date
   * @param extra_metadata
   * @param processing_date
   */
  case class Merchants( merchant_id: String, merchant_name: String, status: String, industry: String, risk_score: BigDecimal, country_code: String, onboard_date: java.sql.Date, extra_metadata: Option[String], processing_date: String )


  /**
   *
   * @param user_id
   * @param user_name
   * @param email
   * @param country_code
   * @param signup_date
   * @param loyalty_tier
   * @param processing_date
   */
  case class Users( user_id: String, user_name: String, email: String, country_code: String, signup_date: java.sql.Date, loyalty_tier: String, processing_date: String )


}
