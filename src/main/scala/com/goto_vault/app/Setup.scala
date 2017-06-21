package com.goto_vault.app

import org.scalatra.{FutureSupport, ScalatraBase, ScalatraServlet}
import slick.jdbc.H2Profile.api._

import com.goto_vault.app.Account
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration

object Setup {

  val db = Database.forConfig("h2mem1")
  val Accounts = TableQuery[AccountTable]
  val Transaction = TableQuery[Transaction]

  def hash(s: String): String = {
    val m = java.security.MessageDigest.getInstance("MD5")
    val b = s.getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(16)
  }


  def primary_setup_account(): Unit = {
    val create_table = DBIO.seq(
      Accounts.schema.create,
      Accounts += (1, "Andrew Tvorozhkov", 0, "1234", "aaa@a.ru"),
      Accounts += (2, "Grisha Belogorov", 0, "1212", "bbb@bb.ru")
    )
    db.run(create_table)
  }

  def primary_setup_transaction(): Unit = {
    val create_table = DBIO.seq(
      Transaction.schema.create,
      Transaction += (1, 1, 2, 100),
      Transaction += (2, 2, 1, 100)
    )
    db.run(create_table)
  }


  def get_last_account(): Int = {
    val query = Accounts.length.result
    println(query)

    def res = Await.result(db.run(query), Duration.Inf)

    res
  }

  def get_last_transaction(): Int = {
    val query = Transaction.length.result

    def res = Await.result(db.run(query), Duration.Inf)

    res
  }

  def add_account(name: String, balance: Double, pass: String, email: String): Unit = {
    val insertActions = DBIO.seq(Accounts += (this.get_last_account() + 1, name, balance, hash(pass), email))
    db.run(insertActions)
  }

  def add_transaction(from: Int, to: Int, amount: Double): Unit = {
    val insertActions = DBIO.seq(Transaction += (this.get_last_transaction() + 1, from, to, amount))
    db.run(insertActions)
  }

  def money_operation(from: Int, to: Int, amount: Double): Unit = {
    money_operation_with_db(from, amount)
    money_operation_with_db(to, -amount)
    add_transaction(from, to, amount)
  }

  def money_operation_with_db(acc_id: Int, amount: Double): Unit = {
    val query = Accounts.filter(_.id === acc_id).map(_.balance).result

    def res: Double = Await.result(db.run(query), Duration.Inf).head

    val q2 = Accounts.filter(_.id === acc_id).map(_.balance).update(res + amount)
    db.run(q2)

    def try_login(email: String, password: String): Boolean = {
      val query = Accounts.filter(_.email === email).map(_.password).result

      def res: String = Await.result(db.run(query), Duration.Inf).toString()

      hash(password) == res
    }

    def get_account_by_email(email: String): Account = {
      val query = Accounts.filter(_.email === email).result

      def res: (Int, String, Double, String, String) = Await.result(db.run(query), Duration.Inf).head
      Account(res._1, res._2, res._3, res._4, res._5)
    }
  }
}