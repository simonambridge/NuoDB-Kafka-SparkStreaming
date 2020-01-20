# NuoDB - Streaming Analytics Demo

## Creating and Consuming Transactions

This project consists of two elements:
   
* Transaction Producer
* Transaction Consumer

The transaction producer is a Scala application that leverages the Akka framework (lightly) to generate pseudo-random credit card transactions, and then place those transactions on a Kafka queue. There is some fairly trivial yet fun logic for spreading the transactions proportionally across the top 100 retailers in the world based on total sales. It does a similar thing for the countries of the world based on population.

The Transaction consumer, also written in Scala, is a Spark streaming job. This job performs two main tasks. 
- First, it consumes the messages put on the Kafka queue.
  - It then parses those messages, evalutes the data and flags each transaction as "APPROVED" or "REJECTED". This is the place in the job where more application specific (or complex) logic should be placed. In a real world application this could be a scoring that could decide whether a transaction should be accepted or rejected. You would also want to implement things like black-list lookups and other validation or analysis. 
  - Finally, once evaluated, the records are then written to the NuoDB ```transactions``` table.

- The second part of the Spark consumer job counts the number of records processed each minute, and stores that data to an aggregates table. The only unique aspect of this flow is that the job also reads back from from this table and builds a rolling count of the data. 


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


You can now return to the main Readme to build and run the streaming demo.
