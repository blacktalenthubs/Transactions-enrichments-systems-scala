package com.myorg

object ScalaFundementals {

  case class Person(name: String, age: Int)

  case class Transaction(
                          transaction_id: String,
                          user_id: String,
                          merchant_id: String,
                          amount: Double,
                          currency: String
                        )


  def main(args: Array[String]): Unit = {

    val matchValue: Option[Int] = Some(5)


    matchValue match {
      case Some(v) => print(v)
      case None => print("No value found")
    }

    val nums = List(1, 2, 3)
    val s = Set("apple", "banana")


    val doubled = nums.map(n => n * 2)
    print(doubled)

    val evens = nums.filter(n => n % 2 == 0)
    print(evens)

    val nested = List(List(1, 2), List(3, 4))
    val flat_map = nested.flatMap(identity)

    print(flat_map)

    val flattened = nested.flatten
    print(flattened)


    val transactions: List[Transaction] = List(
      Transaction("txn_001", "U100", "M200", 29.99, "USD"),
      Transaction("txn_002", "U101", "M200", 15.50, "USD"),
      Transaction("txn_003", "U102", "M201", 42.00, "USD")
    )

    val filtered = transactions.filter(tx => tx.amount >= 20.0)

    filtered.foreach(println)



  }
}
