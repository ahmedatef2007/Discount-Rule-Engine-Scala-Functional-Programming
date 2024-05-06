import java.io.{File, FileOutputStream, PrintWriter}
import java.sql.{Date, DriverManager}
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import scala.io.Source

object main extends App {
  val f: File = new File("src/main/resources/logs.txt")
  val writer = new PrintWriter(new FileOutputStream(f, true))
  log_event(writer, f, "info", "Openning Writer")


  val orders = Source.fromFile("src/main/resources/TRX1000.csv").getLines().toList.tail
  //  orders.foreach(println)
  println(orders(1))
  val ordersWithDiscounts = orders.map(order => get_order_with_discount(order, get_list_of_rules()))
  //ordersWithDiscounts.foreach(println)

  // functions that takes an order and output a bool -> Qualifying Rules
  // functions that takes an order and output a double -> Get Discount
  val ordersdata = orders.map(_.split(","))

  println("more_than_5_qualifier : " + more_than_5_qualifier(orders(1)))
  val url = "jdbc:oracle:thin:@//localhost:1521/XE"
  val username = "AHMED"
  val password = "ahmed"
  val connection = DriverManager.getConnection(url, username, password)

  println("get_more_than_5_discount : " + get_more_than_5_discount(orders(1)))


  println("cheese_and_wine_qualifier : " + cheese_and_wine_qualifier(orders(1)))


  println("get_cheese_and_wine_discount : " + get_cheese_and_wine_discount(orders(1)))

  def get_list_of_rules(): List[(String => Boolean, String => Double)] = {
    List((cheese_and_wine_qualifier, get_cheese_and_wine_discount),
      (more_than_5_qualifier, get_more_than_5_discount),
      (products_sold_23_march_qualifier, get_products_sold_23_march_discount),
      (less_than_30_qualifier_using_days_between, get_less_than_30_qualifier_discount))
  }

  // bought more than 5 of the same product
  def more_than_5_qualifier(order: String): Boolean = {
    val quantity = order.split(",")(3).toInt
    quantity > 5
  }

  println("products_sold_23_march_qualifier : " + products_sold_23_march_qualifier(orders(1)))

  def get_more_than_5_discount(order: String): Double = {
    val quantity = order.split(",")(3).toInt
    if (quantity >= 6 & quantity <= 9) 0.05 //6 – 9 units -> 5% discount
    else if (quantity >= 10 & quantity <= 14) 0.07 //10-14 units -> 7% discount
    else 0.10 //More than 15 -> 10% discount
  }

  println("get_products_sold_23_march_discount : " + get_products_sold_23_march_discount(orders(1)))

  //Cheese and wine products are on sale
  def cheese_and_wine_qualifier(order: String): Boolean = {
    val name = order.split(",")(1)
    name.startsWith("Wine") || name.startsWith("Cheese")
  }

  def get_cheese_and_wine_discount(order: String): Double = {
    val name = order.split(",")(1)
    if (name.startsWith("Wine")) 0.05 //wine -> 5% discount
    else if (name.startsWith("Cheese")) 0.10 //cheese -> 10% discount
    else 0.10 //More than 15 -> 10% discount
  }

  //less than 30 days remaining for the product to expire
  def less_than_30_qualifier_using_days_between(order: String): Boolean = {
    val orderDate = order.split(",")(0).substring(0, 10)
    val expiryDate = order.split(",")(2)

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val orderLocalDate = LocalDate.parse(orderDate, formatter)
    val expiryLocalDate = LocalDate.parse(expiryDate, formatter)

    val daysDifference = java.time.temporal.ChronoUnit.DAYS.between(orderLocalDate, expiryLocalDate)

    daysDifference < 30
  }

  println(ordersdata(1))

  def get_less_than_30_qualifier_discount(order: String): Double
  = {
    val orderDate = order.split(",")(0).substring(0, 10)
    val expiryDate = order.split(",")(2)

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val orderLocalDate = LocalDate.parse(orderDate, formatter)
    val expiryLocalDate = LocalDate.parse(expiryDate, formatter)

    val daysDifference = java.time.temporal.ChronoUnit.DAYS.between(orderLocalDate, expiryLocalDate).toInt

    def calculateDiscount(daysRemaining: Int, acc: Double = 0.0): Double = {
      if (daysRemaining > 30) 0.0 else {
        if (daysRemaining >= 29) {
          acc + 0.01 * (daysRemaining - 28)
        } else {
          calculateDiscount(daysRemaining + 1, acc + 0.01)
        }
      }
    }

    calculateDiscount(daysDifference)
  }

  //Products that are sold on 23rd of March have a special discount!
  def products_sold_23_march_qualifier(order: String): Boolean = {
    val orderDate = order.split(",")(0).substring(0, 10)
    val orderMonth = orderDate.substring(5, 7).toInt
    val orderDay = orderDate.substring(8, 10).toInt

    orderMonth == 3 && orderDay == 23
  }

  def get_products_sold_23_march_discount(order: String): Double = {
    val orderDate = order.split(",")(0).substring(0, 10)
    val orderMonth = orderDate.substring(5, 7).toInt
    val orderDay = orderDate.substring(8, 10).toInt

    if (orderMonth == 3 && orderDay == 23) 0.5
    else 0.0
  }

  def get_order_with_discount(order: String, rules: List[(String => Boolean, String => Double)]): String = {

    val discounts = rules.filter(_._1(order)).map(_._2(order))
    val finalDiscount = if (discounts.isEmpty) 0 else discounts.sum / discounts.length.toDouble

    val quantity = order.split(",")(3).toInt
    val unitPrice = order.split(",")(4).toDouble
    val finalPrice = (unitPrice * quantity) - (unitPrice * quantity * finalDiscount)
    log_event(writer, f, "info", "Order Discount Counted")

    order + "," + finalDiscount + "," + finalPrice
  }

  println(ordersWithDiscounts(1))

  log_event(writer, f, "info", "Opened DB Connection")

  def write_to_db(order: String): Unit = {
    order.split(",").toList match {
      case orderDateStr :: productName :: expiryDateStr :: quantityStr :: unitPriceStr :: channel :: paymentMethod :: discountStr :: finalPriceStr :: Nil =>
        try {
          val orderDate = LocalDate.parse(orderDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          val expiryDate = LocalDate.parse(expiryDateStr)
          val quantity = quantityStr.toInt
          val unitPrice = unitPriceStr.toDouble
          val daysToExpiry = java.time.temporal.ChronoUnit.DAYS.between(orderDate, expiryDate).toInt
          val productCategory = productName.split(" - ")(0)
          val discount = discountStr.toDouble
          val finalPrice = finalPriceStr.toDouble
          val insertStatement =
            """
              |INSERT INTO orders (order_date, expiry_date, days_to_expiry, product_category,
              |                   product_name, quantity, unit_price, channel, payment_method,
              |                   discount, total_price)
              |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              |""".stripMargin

          val preparedStatement = connection.prepareStatement(insertStatement)
          log_event(writer, f, "info", "Order Inserted")

          preparedStatement.setDate(1, Date.valueOf(orderDate.toString))
          preparedStatement.setDate(2, Date.valueOf(expiryDate.toString))
          preparedStatement.setInt(3, daysToExpiry)
          preparedStatement.setString(4, productCategory)
          preparedStatement.setString(5, productName)
          preparedStatement.setInt(6, quantity)
          preparedStatement.setDouble(7, unitPrice)
          preparedStatement.setString(8, channel)
          preparedStatement.setString(9, paymentMethod)
          preparedStatement.setDouble(10, discount)
          preparedStatement.setDouble(11, finalPrice)

          preparedStatement.executeUpdate()

          preparedStatement.close()

        } catch {
          case e: Exception =>
            println(s"Failed to insert order into database: ${e.getMessage}")
            log_event(writer, f, "Error ", e.getMessage)

        }

    }
  }


  def log_event(writer: PrintWriter, file: File, log_level: String, message: String): Unit = {
    writer.write(s"Timestamp: ${Instant.now()}\tLogLevel: ${log_level}\tMessage: ${message}\n")
    writer.flush()
  }

  ordersWithDiscounts.foreach(write_to_db(_))
  log_event(writer, f, "info", "Closed DB Connection")
  connection.close()
  log_event(writer, f, "info", "Closing Writer")

  writer.close()
}
