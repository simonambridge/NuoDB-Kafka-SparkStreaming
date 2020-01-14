# NuoDB Card Fraud Prevention Demo - Streaming Analytics

## Creating and Consuming Transactions

Based on an original creation by Cary Bourgeois. 

This project consists of two elements:
   
* Transaction Producer
* Transaction Consumer

The transaction producer is a Scala application that leverages the Akka framework (lightly) to generate pseudo-random credit card transactions, and then place those transactions on a Kafka queue. There is some fairly trivial yet fun logic for spreading the transactions proportionally across the top 100 retailers in the world based on total sales. It does a similar thing for the countries of the world based on population.

The Transaction consumer, also written in Scala, is a Spark streaming job. This job performs two main tasks. 
- First, it consumes the messages put on the Kafka queue.
  - It then parses those messages, evalutes the data and flags each transaction as "APPROVED" or "REJECTED". This is the place in the job where more application specific (or complex) logic should be placed. In a real world application this could be a scoring that could decide whether a transaction should be accepted or rejected. You would also want to implement things like black-list lookups and other validation or analysis. 
  - Finally, once evaluated, the records are then written to the NuoDB ```transactions``` table.

- The second part of the Spark consumer job counts the number of records processed each minute, and stores that data to an aggregates table. The only unique aspect of this flow is that the job also reads back from from this table and builds a rolling count of the data. 

The aggregated results can be displayed using the Node.js web service provided, for example:

<p align="left">
  <img src="txnchart.png"/>
</p>

## Demo Set Up

### Pre-requisites
The following components must be installed and available on your machine.

NB this demo is built using Spark Direct Streams (Kafka in this demo) 

  1. NuoDB v4.0.x
  2. Apache Kafka 2.4.0, I used the Scala 2.11 build
  3. Apache Spark 2.4.4
  4. Akka 2.6.1
  5. sbt (Scala Build Tool) 1.3.5
  6. An internet connection, required to download dependencies



## 2. Installing and Using Apache Kafka

https://github.com/simonambridge/NCFP/blob/master/TransactionHandlers/NCFP-Kafka-Install.md

## 3. Installing and Using Apache Spark

https://github.com/simonambridge/NCFP/blob/master/TransactionHandlers/NCFP-Spark-Install.md

## 4. Install SBT (Scala Build Tool) and build the demo

If you havent done it already, install sbt (as root or use sudo). You will need sbt to compile the streaming and batch services written in Scala.

On Centos/RHEL (check latest release at https://www.scala-sbt.org/download.html):
```
$ wget http://dl.bintray.com/sbt/rpm/sbt-1.3.5.rpm
```
Install the rpm:
```
$ rpm -qa sbt-1.3.5.rpm
```

Run sbt update to download the libraries and dependencies (may take a little while):
```
$ sbt update
```

When you first compile with sbt it may take some time to download the libraries and packages required.


### Build the packages

* You should have already created the NuoDB tables using the creates_and_inserts.cql script
* All components should be working e.g. Zookeeper, Kafka

> If you are restarting the demo you can clear the tables using the clear_tables.cql script

1. Navigate to the project TransactionHandlers directory:

    ```
    $ cd <path>/NuoDB-Card-Fraud-Prevention/TransactionHandlers
    ```
    
    Update sbt (may take a little while):

    ```
    $ sbt update
    [info] Done updating.
    [success] Total time: 8 s, completed 20-Dec-2019 01:19:04

    ```
    
    
    Clean up the build environment:

    ```
    $ sbt clean
    [info] Set current project to transactionhandlers (in build file:/Users/sambridge/Documents/Projects/GitHub/NuoDB-Card-Fraud-Prevention/TransactionHandlers/)
    [success] Total time: 0 s, completed 20-Dec-2019 01:20:23
    ```


2. Build the Producer app with this command:
  
    ```$ sbt producer/package```
    
    Make sure the build is successful:
    ```
    [info] Done packaging.
    [success] Total time: 5 s, completed 30-Dec-2019 15:31:16
    ```

    > If there are any errors reported, you must resolve them before continuing.


3. Build the Consumer app with this command:
  
    ```
    $ sbt consumer/package
    ```
    
    Make sure the build is successful:
    ```
    [info] Done packaging.
    [success] Total time: 14 s, completed 30-Dec-2019 15:34:42
    ```
    > If there are any errors reported, you must resolve them before continuing.
    

## Run the demo

  At this point the code has compiled successfully.

  The next step is to start the producer and consumer to start generating and receiving transactions.

### Start the Transaction Producer app

From the root directory of the project (`<NCFP install path>/NCFP/TransactionHandlers`) start the producer app:
  
Navigate to the TransactionHandlers directory
```
$ cd /<NCFP install path>/NCFP/TransactionHandlers
```

Start the producer app:
```
$ sbt producer/run
```

After some initial output you will see card transactions being created and posted to Kafka:
```
[info] Set current project to transactionhandlers (in build file:/u02/dev/dse_dev/RTFAP/RTFAP2/TransactionHandlers/)
[info] Running TransactionProducer 
kafkaHost 127.0.0.1:9092
kafkaTopic NewTransactions
maxNumTransPerWait 5
waitMillis 500
runDurationSeconds -1
[DEBUG] [11/09/2017 22:31:09.061] [run-main-0] [EventStream(akka://TransactionProducer)] logger log1-Logging$DefaultLogger started
[DEBUG] [11/09/2017 22:31:09.063] [run-main-0] [EventStream(akka://TransactionProducer)] Default Loggers started
...
(cc_no=,2251000088321444, txn_time=,2017-11-09 22:31:09.735, items=,Item_28750->128.66, amount=,128.66)
(cc_no=,9065000095025035, txn_time=,2017-11-09 22:31:09.735, items=,Item_94332->246.57,Item_16778->708.27,Item_89204->541.07, amount=,1495.91)
(cc_no=,2195000009045559, txn_time=,2017-11-09 22:31:09.735, items=,Item_92502->194.22, amount=,194.22)
196 Transactions created.
(cc_no=,3531000024839480, txn_time=,2017-11-09 22:31:10.237, items=,Item_88890->564.97,Item_99369->404.01,Item_38980->827.30, amount=,1796.28)
(cc_no=,7017000098496305, txn_time=,2017-11-09 22:31:10.238, items=,Item_45228->447.20, amount=,447.20)
(cc_no=,8139000079813195, txn_time=,2017-11-09 22:31:10.238, items=,Item_56710->746.63,Item_76484->793.98, amount=,1540.61)
201 Transactions created.
...
```

Leave this job running to continue to generate transactions.

The records for each transaction are consumed and stored in a NuoDB table.
You can view the records that are being created. Start NuoSQL and run the following query:
```
SQL> select * from transactions;

cc_no            | year | month | day | txn_time                        | amount  | cc_provider | country | date_text | hour | items | location | merchant                  | min | notes | solr_query | status   | tags | txn_id                               | user_id
------------------+------+-------+-----+---------------------------------+---------+-------------+---------+-----------+------+-------+----------+---------------------------+-----+-------+------------+----------+------+--------------------------------------+---------
 1640000007096559 | 2017 |    11 |   5 | 2017-12-05 21:19:24.096000+0000 | 1044.02 |        1640 |      CN |  20171105 |    9 |  null |          |           AVB Brandsource |  19 |  null |       null | APPROVED | null | e96da250-0c58-4d9e-9765-b331c6d6fcc2 |    null
 1847000039868834 | 2017 |    11 |   5 | 2017-12-05 21:20:20.365000+0000 | 1535.59 |        1847 |      NG |  20171105 |    9 |  null |          |           Wal-Mart Stores |  20 |  null |       null | REJECTED | null | cd9c157c-56ff-4799-9511-37a680f7e817 |    null
```
The number of records will constantly increase - re-run this command in cqlsh a few times:
```
SQL> select count (*) from rtfap.transactions ;

 count
-------
  3832

(1 rows)
```


### Start the Transaction Consumer app
 
  1. Start the consumer app.
  
  Navigate to the TransactionHandlers directory
  ```
  $ cd /<RTFAP2 install path>/RTFAP2/TransactionHandlers
  ```
  
  > You no longer need to specify a spark master address in the dse spark-submit command:
  
  Use the dse command to submit a spark job:
  ```
  $ spark-submit --jars /Users/simonambridge/.ivy2/cache/com.typesafe/config/bundles/config-1.3.2.jar --packages org.apache.spark:spark-streaming-kafka_2.11:1.6.3 --class TransactionConsumer consumer/target/scala-2.11/consumer_2.11-0.1.jar
  ```

  After some initial output you will see records being consumed from Kafka by Spark:
  
  ```
  Ivy Default Cache set to: /home/dse/.ivy2/cache
  The jars for the packages stored in: /home/dse/.ivy2/jars
  :: loading settings :: url = jar:file:/usr/share/dse/spark/lib/ivy-2.4.0.jar!/org/apache/ivy/core/settings/ivysettings.xml
  org.apache.spark#spark-streaming-kafka_2.10 added as a dependency
  :: resolving dependencies :: org.apache.spark#spark-submit-parent;1.0
  ...
  6 rows processed...
  +----------------+-----------+----+-----+---+----+---+--------------------+--------------------+----------------+--------+-------+-------+--------+---------+
  |           cc_no|cc_provider|year|month|day|hour|min|            txn_time|              txn_id|        merchant|location|country| amount|  status|date_text|
  +----------------+-----------+----+-----+---+----+---+--------------------+--------------------+----------------+--------+-------+-------+--------+---------+
  |1567000016674783|       1567|2016|   11|  3|   0| 37|2016-12-03 00:37:...|e4425655-348c-47d...|Lowe's Companies|        |     KH|1153.26|APPROVED| 20161103|
  |8797000077172306|       8797|2016|   11|  3|   0| 37|2016-12-03 00:37:...|abeae2c2-173c-429...|       SUPERVALU|        |     DE| 812.28|APPROVED| 20161103|
  |5034000081986740|       5034|2016|   11|  3|   0| 37|2016-12-03 00:37:...|afa129ee-6829-4b0...|  Dollar General|        |     IN|1324.22|REJECTED| 20161103|
  |5859000021039989|       5859|2016|   11|  3|   0| 37|2016-12-03 00:37:...|ef372bb1-dedf-421...|          Costco|        |     ID| 757.88|APPROVED| 20161103|
  +----------------+-----------+----+-----+---+----+---+--------------------+--------------------+----------------+--------+-------+-------+--------+---------+

  4 rows processed...
  +----------------+-----------+----+-----+---+----+---+--------------------+--------------------+---------------+--------+-------+-------+--------+---------+
  |           cc_no|cc_provider|year|month|day|hour|min|            txn_time|              txn_id|       merchant|location|country| amount|  status|date_text|
 +----------------+-----------+----+-----+---+----+---+--------------------+--------------------+---------------+--------+-------+-------+--------+---------+
  |5907000019173296|       5907|2016|   11|  3|   0| 37|2016-12-03 00:37:...|93c3741b-5132-49e...|     DineEquity|        |     EG| 896.46|APPROVED| 20161103|
  |7624000055927622|       7624|2016|   11|  3|   0| 37|2016-12-03 00:37:...|e7a09e38-543b-4bf...|     Albertsons|        |     FR|1944.73|APPROVED| 20161103|
  |5539000022858144|       5539|2016|   11|  3|   0| 37|2016-12-03 00:37:...|a2aefc67-c97c-48e...|Jack in the Box|        |     CN|2491.93|APPROVED| 20161103|
  +----------------+-----------+----+-----+---+----+---+--------------------+--------------------+---------------+--------+-------+-------+--------+---------+
  ```

  Leave this job running to continue to process transactions.

  2. At this point you can use cqlsh to check the number of rows in the Transactions table - you should see that there are records appearing as they are posted by the consumer process:

  ```
  cqlsh> select count(*) from rtfap.transactions;

   count
  -------
    13657
  ```

  3. Every 60 seconds you will also see the consumer process generate output similar to the following:
```
  Time=Sat Dec 03 00:37:44 GMT 2016
  +----+-----+---+----+------+-------------------+-----------------+-----------+----------------+-----------------+----------+---------------+
  |year|month|day|hour|minute|               time|approved_rate_min|ttl_txn_min|approved_txn_min| approved_rate_hr|ttl_txn_hr|approved_txn_hr|
  +----+-----+---+----+------+-------------------+-----------------+-----------+----------------+-----------------+----------+---------------+
  |2016|   12|  3|   0|    37|2016-12-03 00:37:44|94.55958549222798|        386|             365|95.27943966146213|     13706|          13059|
  +----+-----+---+----+------+-------------------+-----------------+-----------+----------------+-----------------+----------+---------------+
  ```

  This is real-time analysis of the approved vs. rejected transactions rate and percentage. 
  
  These records are stored in the ```txn_count_min``` table, for example:
  
  ```
  cqlsh:rtfap> SELECT * FROM rtfap.txn_count_min WHERE solr_query = '{"q":"*:*",  "fq":"time:[NOW-1HOUR TO *]","sort":"time asc"}';

   year | month | day | hour | minute | approved_rate_hr | approved_rate_min | approved_txn_hr | approved_txn_min | solr_query | time                     | ttl_txn_hr | ttl_txn_min
  +-----+-------+-----+------+--------+------------------+-------------------+-----------------+------------------+------------+--------------------------+------------+-------------+
  2016 |    12 |   2 |   23 |     43 |          95.7958 |           95.7958 |             319 |              319 |       null | 2016-12-02 23:43:44+0000 |        333 |         333
  2016 |    12 |   2 |   23 |     44 |         94.86405 |          93.92097 |             628 |              309 |       null | 2016-12-02 23:44:44+0000 |        662 |         329
  2016 |    12 |   2 |   23 |     45 |         94.90695 |          94.98607 |             969 |              341 |       null | 2016-12-02 23:45:44+0000 |       1021 |         359
  2016 |    12 |   2 |   23 |     46 |         94.76015 |          94.31138 |            1284 |              315 |       null | 2016-12-02 23:46:44+0000 |       1355 |         334
  2016 |    12 |   2 |   23 |     47 |         94.72769 |          94.60916 |            1635 |              351 |       null | 2016-12-02 23:47:44+0000 |       1726 |         371
  2016 |    12 |   2 |   23 |     48 |         94.77218 |          94.98607 |            1976 |              341 |       null | 2016-12-02 23:48:44+0000 |       2085 |         359
  2016 |    12 |   2 |   23 |     49 |         94.65021 |          93.91304 |            2300 |              324 |       null | 2016-12-02 23:49:44+0000 |       2430 |         345
  2016 |    12 |   2 |   23 |     50 |         94.53041 |          93.69628 |            2627 |              327 |       null | 2016-12-02 23:50:44+0000 |       2779 |         349
  2016 |    12 |   2 |   23 |     51 |         94.56522 |          94.84241 |            2958 |              331 |       null | 2016-12-02 23:51:44+0000 |       3128 |         349
  2016 |    12 |   2 |   23 |     52 |         94.61231 |                95 |            3319 |              361 |       null | 2016-12-02 23:52:44+0000 |       3508 |         380
  2016 |    12 |   2 |   23 |     53 |          94.5497 |          93.91304 |            3643 |              324 |       null | 2016-12-02 23:53:44+0000 |       3853 |         345
  2016 |    12 |   2 |   23 |     54 |         94.64328 |          95.62842 |            3993 |              350 |       null | 2016-12-02 23:54:44+0000 |       4219 |         366
  2016 |    12 |   2 |   23 |     55 |         94.65381 |          94.78261 |            4320 |              327 |       null | 2016-12-02 23:55:44+0000 |       4564 |         345
  2016 |    12 |   2 |   23 |     56 |         94.63454 |              94.4 |            4674 |              354 |       null | 2016-12-02 23:56:44+0000 |       4939 |         375
  2016 |    12 |   2 |   23 |     57 |         94.67097 |          95.20958 |            4992 |              318 |       null | 2016-12-02 23:57:44+0000 |       5273 |         334
  2016 |    12 |   2 |   23 |     58 |         94.60566 |          93.60465 |            5314 |              322 |       null | 2016-12-02 23:58:44+0000 |       5617 |         344
  2016 |    12 |   2 |   23 |     59 |         94.63087 |          95.04373 |            5640 |              326 |       null | 2016-12-02 23:59:44+0000 |       5960 |         343
  2016 |    12 |   3 |    0 |      0 |         96.73913 |          96.73913 |             356 |              356 |       null | 2016-12-03 00:00:44+0000 |        368 |         368
  2016 |    12 |   3 |    0 |      1 |         96.32249 |          95.87021 |             681 |              325 |       null | 2016-12-03 00:01:44+0000 |        707 |         339
  2016 |    12 |   3 |    0 |      2 |         96.13936 |          95.77465 |            1021 |              340 |       null | 2016-12-03 00:02:44+0000 |       1062 |         355
  2016 |    12 |   3 |    0 |      3 |         96.11033 |          96.02273 |            1359 |              338 |       null | 2016-12-03 00:03:44+0000 |       1414 |         352
  ```


  Data in the ```txn_count_min``` table will be used to service a D3 chart.
  
  > Remember - there is a TTL on the transactions table, so the data will gradually age out after 24 hours!! :)
  
  > Note - if you get an error like this on a single-node install it may because you've exceeded the numbner of tombstones:
  ```
  ReadFailure: Error from server: code=1300 [Replica(s) failed to execute read] message="Operation failed - received 0 responses and 1 failures" info={'failures': 1, 'received_responses': 0, 'error_code_map': {'127.0.0.1': '0x0001'}, 'required_responses': 1, 'consistency': 'ONE'}
  ```
  In this case run the following command in cqlsh:
  ```
  cqlsh:rtfap> ALTER TABLE transactions WITH GC_GRACE_SECONDS = 0;
  ```
  After this trigger a compaction via the nodetool to flush out all the tombstones.
  On a one-node-cluster you can leave  GC_GRACE_SECONDS at zero but keep in mind change this back if you plan to use more than one node.


  4. View the transaction approval data as a graph
  
  Go to the service URL: http://localhost:3000/
  
  Select the Transaction Chart option. After a while you should see something similar to the following:
  
  <p align="left">
    <img src="txnchart2.png"/>
  </p>

You could add more detail to this chart if you wish.
