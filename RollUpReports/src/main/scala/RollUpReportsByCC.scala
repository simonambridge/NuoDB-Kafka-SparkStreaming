/**
 * Created by Kkusoorkar-MBP15 on 3/18/16.
 */

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SaveMode}


object RollUpReportsByCC {

  def main (args: Array[String]){
    println("Beginning RollUp Reporting By Credit Card number...")

    //      val conf = new SparkConf().setAppName("RollUpReportsByMerchant
    //      specify a different spark ui port to avoid conflict with other spark processes acrive e.g. Zeppelin
    val conf = new SparkConf().setAppName("RollUpReportsByCard").set("spark.ui.port", "40400" ).set("spark.driver.allowMultipleContexts", "true")

    val sc = SparkContext.getOrCreate(conf)
    val sqlContext = new HiveContext(sc)

    sqlContext.sql("""CREATE TEMPORARY VIEW temp_transactions
      USING org.apache.spark.sql.cassandra
      OPTIONS (
       table "transactions",
       keyspace "rtfap",
       cluster "Test Cluster",
       pushdown "true"
      )""")

    // convert integer date elements to string - just a subset of fields for the CC roll-ups
    val rollupDF = sqlContext.sql("SELECT cc_no, cast(year as String), cast(month AS String), cast(day AS String), cast(hour AS String),amount FROM temp_transactions")
    // Create a temp table based on the new DF
    rollupDF.registerTempTable("my_transactions")

    // 1. hourlyaggregates_bycc
    println(" - 1. Populating hourlyaggregates_bycc")

    val rollup1= sqlContext.sql("select cc_no, " +
      "int(concat(year, if(length(month)=1, concat('0',month), month),if(length(day)=1, concat('0',day), day), if(length(hour)=1, concat('0',hour), hour))) as hour, " +
      "sum(amount) as total_amount, " +
      "min(amount) as min_amount, " +
      "max(amount) as max_amount, " +
      "count(*) as total_count " +
      "from my_transactions " +
      "group by cc_no, " +
      "concat(year, if(length(month)=1, concat('0',month), month),if(length(day)=1, concat('0',day), day), if(length(hour)=1, concat('0',hour), hour))")

    rollup1.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "hourlyaggregates_bycc", "confirm.truncate" -> "true"))
      .save()

    // 2. dailyaggregates_bycc
    println(" - 2. Populating dailyaggregates_bycc")
    val rollup2= sqlContext.sql("select cc_no, " +
      "int(concat(year, if(length(month)=1, concat('0',month), month),if(length(day)=1, concat('0',day), day))) as day, " +
      "sum(amount) as total_amount, " +
      "min(amount) as min_amount, " +
      "max(amount) as max_amount, " +
      "count(*) as total_count " +
      "from my_transactions " +
      "group by cc_no, " +
      "concat(year, if(length(month)=1, concat('0',month), month),if(length(day)=1, concat('0',day), day))")

    rollup2.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "dailyaggregates_bycc", "confirm.truncate" -> "true"))
      .save()

    // 3. monthlyaggregates_bycc
    println(" - 3. Populating monthlyaggregates_bycc")
    val rollup3= sqlContext.sql("select cc_no, " +
      "int(concat(year, if(length(month)=1, concat('0',month), month))) as month, " +
      "sum(amount) as total_amount, " +
      "min(amount) as min_amount, " +
      "max(amount) as max_amount, " +
      "count(*) as total_count " +
      "from my_transactions " +
      "group by cc_no, concat(year, if(length(month)=1, concat('0',month), month))")

    rollup3.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "monthlyaggregates_bycc", "confirm.truncate" -> "true"))
      .save()

    // 4. yearlyaggregates_bycc
    println(" - 4. Populating yearlyaggregates_bycc")
    val rollup4= sqlContext.sql("select cc_no, " +
      "int(year) as year, " +
      "sum(amount) as total_amount, " +
      "min(amount) as min_amount, " +
      "max(amount) as max_amount, " +
      "count(*) as total_count " +
      "from my_transactions group by cc_no, int(year)")

    rollup4.write.format("org.apache.spark.sql.cassandra")
      .mode(SaveMode.Overwrite)
      .options(Map("keyspace" -> "rtfap", "table" -> "yearlyaggregates_bycc", "confirm.truncate" -> "true"))
      .save()


    println("Completed RollUps By CC")

    println("Shutting down...")

    sc.stop();
    
  }
}
