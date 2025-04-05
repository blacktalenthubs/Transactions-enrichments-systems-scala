package com.streams

import com.github.javafaker.Faker
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.Properties
import java.util.Locale
import scala.util.Random

object ContinuousTransactionsProducer {

  case class Transactions(
                           tx_id: String,
                           user_id: String,
                           merchant_id: String,
                           amount: BigDecimal,
                           currency: String,
                           status: String,
                           failure_reason: Option[String],
                           payment_method: String
                         )

  // Simple transaction generator
  def generateTransactions(numTx: Int): Seq[Transactions] = {
    val faker          = new Faker(new Locale("en-US"))
    val rnd            = new Random()
    val paymentStatus  = Array("COMPLETED", "DECLINED", "PENDING", "REFUNDED", "FAILED")
    val paymentMethods = Array("CREDIT_CARD", "PAYPAL", "APPLE_PAY", "GOOGLE_PAY")
    val failureReasons = Array("InsufficientFunds","ExpiredCard","None","LimitExceeded")

    (1 to numTx).map { _ =>
      val txId   = "txn_" + rnd.alphanumeric.take(8).mkString
      val userId = "U" + (1000 + rnd.nextInt(9000))
      val merchId= "M" + (1000 + rnd.nextInt(9000))

      // random 1.0 -> 999.99
      val amtRaw = 1.0 + rnd.nextDouble()*999.0
      val amt    = BigDecimal(amtRaw).setScale(2, BigDecimal.RoundingMode.HALF_UP)

      val status = paymentStatus(rnd.nextInt(paymentStatus.length))
      val reason = if (status == "DECLINED" || status == "FAILED")
        Some(failureReasons(rnd.nextInt(failureReasons.length)))
      else
        None

      val pm     = paymentMethods(rnd.nextInt(paymentMethods.length))

      Transactions(txId, userId, merchId, amt, "USD", status, reason, pm)
    }
  }

  def main(args: Array[String]): Unit = {

    // --------------------------------------------------------------------------------
    // 1) Configure Kafka producer
    // --------------------------------------------------------------------------------
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val kafkaTopic = "transactions_topic"
    val producer   = new KafkaProducer[String, String](props)

    // --------------------------------------------------------------------------------
    // 2) Loop to continuously send transactions every 1 minutes
    // --------------------------------------------------------------------------------
    val intervalMillis = 1 * 60 * 1000 // 10 minutes in ms

    println("Starting continuous producer. Press Ctrl+C to stop.")
    while(true) {
      // Generate some transactions (e.g., 50)
      val txns = generateTransactions(200)

      // Convert to JSON and send to Kafka
      txns.foreach { tx =>
        val txJson =
          s"""
             |{
             |  "tx_id": "${tx.tx_id}",
             |  "user_id": "${tx.user_id}",
             |  "merchant_id": "${tx.merchant_id}",
             |  "amount": ${tx.amount},
             |  "currency": "${tx.currency}",
             |  "status": "${tx.status}",
             |  "failure_reason": ${tx.failure_reason.map(r => s""""$r"""").getOrElse("null")},
             |  "payment_method": "${tx.payment_method}"
             |}
             |""".stripMargin

        val record = new ProducerRecord[String, String](kafkaTopic, tx.tx_id, txJson)
        producer.send(record)
      }

      // Log batch info
      println(s"Sent ${txns.size} transactions to Kafka topic '$kafkaTopic'. Next batch in 10 minutes.")

      // Sleep for 10 minutes before the next batch
      Thread.sleep(intervalMillis)
    }

    // This code won't be reached due to the infinite loop,
    // but in a real app you might handle graceful shutdown:
    // producer.close()
  }
}
