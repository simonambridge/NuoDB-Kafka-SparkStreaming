/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.Timestamp
import java.util.{Calendar, GregorianCalendar}
// added for time conversion in txn_count_min
import com.typesafe.config.ConfigFactory
import kafka.serializer.StringDecoder
import org.apache.spark.sql.functions.unix_timestamp
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext, Time}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession /* required for spark.read */

// This implementation uses the Kafka Direct API supported in Spark 1.4+
object TransactionConsumer extends App {

  /*
   * Get runtime properties from application.conf
   */
  val systemConfig   = ConfigFactory.load()

  val appName        = systemConfig.getString("TransactionConsumer.sparkAppName")

  val kafkaHost      = systemConfig.getString("TransactionConsumer.kafkaHost")
  val kafkaDataTopic = systemConfig.getString("TransactionConsumer.kafkaDataTopic")

  // this does not work in this example at this point as these variables appear to be out of scope for the
  // map portion of the forEachRDD
  //
  val pctTransactionToDecline = systemConfig.getString("TransactionConsumer.pctTransactionToDecline")

  val Keyspace    = systemConfig.getString("TransactionConsumer.Schema")
  val DetailTable = systemConfig.getString("TransactionConsumer.DetailTable")
  val AggTable    = systemConfig.getString("TransactionConsumer.AggTable")

  // val conf        = new SparkConf()
  //                        .setAppName(appName)
  //                        .set("spark.master", "local[*]")
  //                        .set("spark.cores.max", "2")
  //                        .set("spark.executor.memory", "2G")
  //                        .setMaster(“spark://master:7077”)

  // val sqlContext  = SQLContext.getOrCreate(sc)
  // import sqlContext.implicits._

  // New SparkSession for Spark 2+
  val spark       = SparkSession
                      .builder()
                      .appName(appName)
                      .config("spark.master", "local[2]")
                      .config("spark.executor.memory", "2G")
                      .config("spark.cores.max", "2")
                      //.setMaster("local") 
                      //.setMaster("spark://" + sparkMasterHost + ":7077")
                      //.setMaster("local[*]")
                      .getOrCreate

  spark.sparkContext.setLogLevel("WARN")
  
  import spark.implicits._
  
  val sc          = spark.sparkContext
  val ssc         = new StreamingContext(sc, Seconds(1))
  ssc.checkpoint(appName)



  val kafkaParams = Map[String, String]("metadata.broker.list" -> kafkaHost)
  val kafkaTopics = Set(kafkaDataTopic)

  val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, kafkaTopics)

  case class Transaction(cc_no:String,
                         cc_provider: String,
                         year: Int,
                         month: Int,
                         day: Int,
                         hour: Int,
                         min: Int,
                         txn_time: Timestamp,
                         txn_id: String,
                         merchant: String,
                         location: String,
                         country: String,
                         amount: Double,
                         status: String,
                         date_test: String)

  case class TransCount(status: String)

  val jdbcHostname = "localhost"
  val jdbcPort = 48004
  val jdbcDatabase = "hockey"
  val jdbcUsername = "dba"
  val jdbcPassword = "dba"
  val jdbcSchema   = "user"
  // Create the JDBC URL without passing in the user and password parameters.
  val jdbcUrl = s"jdbc:com.nuodb://${jdbcHostname}:${jdbcPort}/${jdbcDatabase}"
  
  // Create a Properties() object to hold the parameters.

  // val connProperties = new Properties()
  // connProperties.put("user", s"${jdbcUsername}")
  // connProperties.put("password", s"${jdbcPassword}")
  // connProperties.put("schema", s"${jdbcSchema}")*/

  /*
   * This stream handles the immediate stream of data to the DB
   */
  kafkaStream.window(Seconds(1), Seconds(1))
    .foreachRDD {
      /*
       * This section down to the .toDF call is where the records from Kafka are consumed and parsed
       */
      (message: RDD[(String, String)], batchTime: Time) => {
        val df = message.map {
          case (k, v) => v.split(";")
        }.map(payload => {
          val cc_no = payload(0)
          val cc_provider = payload(1)

          val txn_time = Timestamp.valueOf(payload(2))
          val calendar = new GregorianCalendar()
          calendar.setTime(txn_time)

          val year = calendar.get(Calendar.YEAR)
          val month = calendar.get(Calendar.MONTH)
          val day = calendar.get(Calendar.DAY_OF_MONTH)
          val hour = calendar.get(Calendar.HOUR)
          val min = calendar.get(Calendar.MINUTE)

          val txn_id = payload(3)
          val merchant = payload(4)
          val location = payload(5)
          val country = payload(6)

          // 
          // no longer using the map data type
          // in future version split out the items map to the  TransactionItems table
          //val items = payload(6).split(",").map(_.split("->")).map { case Array(k, v) => (k, v.toDouble) }.toMap
          //
          val amount = payload(8).toDouble

          //
          // In a real app this should need to be updated to include more evaluation rules.
          //
          val initStatus = payload(9).toInt
          val status = if (initStatus < 5) s"REJECTED" else s"APPROVED"

          val date_text = f"$year%04d$month%02d$day%02d"

          Transaction(cc_no, cc_provider, year, month, day, hour, min, txn_time, txn_id, merchant, location, country, amount, status, date_text)
        }).toDF("cc_no", "cc_provider", "year", "month", "day", "hour", "min","txn_time", "txn_id", "merchant", "location", "country", "amount", "status", "date_text")
  
        /*
         * The NuoDB JDBC driver makes it very simple to write out the records to NuoDB
         */
        df
          .write
          .format("jdbc")
          .option("url", jdbcUrl+"?user=dba&password=dba&schema=user")
          .option("dbtable", DetailTable)
          .mode(SaveMode.Append)
          .save()

        df.show(5)
        println(s"${df.count()} rows processed...")
        // println("<<<Debug message>>>")
      }
    }


  /*
   * This stream handles the one hour roll up every minute
   */
  kafkaStream.window(Minutes(1), Seconds(60))
    .foreachRDD {
      /*
       * Here we take the records and parse just the last value to be able to count them.
       * NOTE: we re-apply the score here which is hugely inefficient and needs to be worked out in a btter way.
       */
      (message: RDD[(String, String)], batchTime: Time) => {
        val df = message.map {
          case (k, v) => v.split(";")
        }.map(payload => {
          val initStatus = payload(9).toInt
          val status = if (initStatus < 5) s"REJECTED" else s"APPROVED"

          TransCount(status)
        }).toDF("status")

        /*
         * The next several section set up all the data arithmetic so we can correctly query the
         * aggregate table and write back to it.
         */
        val timeInMillis = System.currentTimeMillis()

        val currCal = new GregorianCalendar()
        currCal.setTime(new Timestamp(timeInMillis))
        val year = currCal.get(Calendar.YEAR)
        val month = currCal.get(Calendar.MONTH) + 1 // Gregorian months are 0-11
        val day = currCal.get(Calendar.DAY_OF_MONTH)
        val hour = currCal.get(Calendar.HOUR_OF_DAY)
        val min = currCal.get(Calendar.MINUTE)
        val sec = currCal.get(Calendar.SECOND)


        val prevCal = new GregorianCalendar()
        prevCal.setTime(new Timestamp(timeInMillis))
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH) + 1 // Gregorian months are 0-11
        val prevDay = prevCal.get(Calendar.DAY_OF_MONTH)
        val prevHour = prevCal.get(Calendar.HOUR_OF_DAY)
        val prevMin = prevCal.get(Calendar.MINUTE) -1

        /*
         * Count the records in the resulting Dataframe
        */
        val totalTxnMin = df.count()
        val approvedTxnMin = df.filter("status = 'APPROVED'").count()
        val pctApprovedMin = if (totalTxnMin > 0) ((approvedTxnMin/totalTxnMin.toDouble)*100.0) else 0.0

        /*
         * Read from the aggregate table to get the value from the previous minute.
         */

       val dfPrev = spark.read
          .format("jdbc")
          .option("url",jdbcUrl+"?user=dba&password=dba&schema=user")
          .option("dbtable",AggTable)
          //.option("query", "select x, y from z")
          .load()



       dfPrev.registerTempTable("prevRecord");
       var result = spark.sql ("select ttl_txn_hr, approved_txn_hr " +
           "from prevRecord " +
           " where year = " + prevYear +
           " and month = " + prevMonth +
           " and day = " + prevDay +
           " and hour = " + prevHour +
           " and minute = " + prevMin
       )
       // debug:
       // println("prevTime: Year="+prevYear+" Month="+prevMonth+" Hour="+prevHour+" Min="+prevMin)


       //  val result = dfPrev
       //        .filter(s"year = ${prevYear} and month = ${prevMonth} and day = ${prevDay} and hour = ${prevHour} and minute = ${prevMin}")
       //        .select("ttl_txn_hr", "approved_txn_hr")

        val totalTxnHr = totalTxnMin + (if (result.count() > 0) result.first.getInt(0) else 0)
        val approvedTxnHr = approvedTxnMin + (if (result.count() > 0) result.first.getInt(1) else 0)
        val pctApprovedHr = if (totalTxnHr > 0) ((approvedTxnHr/totalTxnHr.toDouble)*100.0) else 0.0

        // debug:
        // println("ttl_txn_hr="+(if (result.count() > 0) result.first.getInt(0) else 0))

        // Format a timestamp value

        var yearString=year.toString()
        var monthString=month.toString()
        if (month<10) { monthString="0"+monthString }
        var dayString=day.toString()
        if (day<10) { dayString="0"+dayString }
        var hourString=hour.toString
        if (hour<10) { hourString="0"+hourString }
        var minString=min.toString()
        if (min<10) { minString="0"+minString }
        var secString=sec.toString()
        if (sec<10) { secString="0"+secString }

        //        format.format(new java.util.Date())
        //        val time = year+"-"+monthString+"-"+dayString+"T"+hourString+":"+minString+":"+secString+"Z"
        val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val tempString = yearString+"-"+monthString+"-"+dayString+" "+hourString+":"+minString+":"+secString
        val timeString = format.parse(tempString)
        println("Time="+timeString) // gives Time=Fri Nov 25 23:53:45 GMT 2016
        // val time = timeString

        val time = format.format(timeString);
        // println("Time="+time)   // gives Time=2016-11-25 23:53:45


        // old - original
        // val tsTime = unix_timestamp(time, "MM/dd/yyyy HH:mm:ss").cast("timestamp")

        /*
         * Make a new DataFrame with our results
         */
        val dfCount = spark.sparkContext.makeRDD(Seq((year, month, day, hour, min, time, pctApprovedMin, totalTxnMin, approvedTxnMin, pctApprovedHr, totalTxnHr, approvedTxnHr)))
          .toDF("year", "month", "day", "hour", "minute", "time", "approved_rate_min", "ttl_txn_min", "approved_txn_min", "approved_rate_hr", "ttl_txn_hr", "approved_txn_hr")

        /*
         * show and write the results out to the aggregate table.
         */
        dfCount.show()

        dfCount
          .write
          .format("jdbc")
          .option("url", jdbcUrl+"?user=dba&password=dba&schema=user")
          .option("dbtable", AggTable)
          .mode(SaveMode.Append)
          .save()
      }
    }


  ssc.start()
  ssc.awaitTermination()
}
