# NuoDB Streaming Data Pipeline Example

How to use modern, real-time, distributed SQL and streaming technologies to build the foudnation for a scalable data pipeline. 
This demo will use these technologies:

- NuoDB
- Spark
- Scala
- Akka


<p>
<p align="center">
  <img src="NCFP-architecture.png"/>
</p>


## Step 1: Install Pre-Requisites
  - A NuoDB 4.x database to run your queries against.
    - Hardware and software requirements for NuoDB are listed here - http://doc.nuodb.com/Latest/Content/System-Requirements.htm
    - Installation and Deployment options are described here - http://doc.nuodb.com/Latest/Content/Deployment-models.htm
    - You can download the NuoDB Community Edition binaries for various platforms here - https://www.nuodb.com/dev-center/community-edition-download
  - A machine on which to install Kafka, Spark and Zeppelin
    - This could be one of the NuoDB Transaction Engines (TE's), or a separate machine/cloud instance, or even a laptop
    - Ideally these services should be in close proximity to the data source in order to reduce latency

This demo uses an installation running on AWS

### URLs
After Spark has been installed the Spark UI URL will be:
- Spark Master: http://[NuoDB_NODE_IP]:7080/ e.g. ```http://hostname:7080/```

When the Node.js ReST service has been created:
- Node.js ReST: http://[NuoDB_NODE_IP]:3000 e.g. ```http://hostname:3000``` 

(where [NuoDB_NODE_IP] is the public IP address of your single node test NuoDB installation)



## Step 2: Clone the NCFP repository

The first step is to clone this repo to a directory on the machine where you have previously installed NuoDB as described above:
```
$ git clone https://github.com/simonambridge/NCFP
```


## Step 3: Data Model

To create this schema and the tables described below, run the create schema script:
```
nuosql <db-name> --user <username> --password <password>
```

For example:
To create the application schema run the create schema script:
```
nuosql hockey --user dba --password dba
```

Next, run the schema creation script itself:
```
SQL> @creates_and_inserts.sql
```

This creates the following tables:

- transactions           - the main transactions table
- hourlyaggregates_bycc  - hourly roll-up of transactions by credit card
- dailyaggregates_bycc   - daily roll-up of transactions by credit card
- monthlyaggregates_bycc - monthly roll-up of transactions by credit card
- yearlyaggregates_bycc  - yearly roll-up of transactions by credit card
- dailytxns_bymerchant   - daily roll-up of transactions by merchant
- txn_count_min          - track transactions in a rolling window for analytics

The create script also creates some sample data for example:

```
INSERT INTO transactions (year, month, day, hour, min, txn_time, cc_no, amount, cc_provider, location, merchant, notes, status, txn_id, user_id, tag)
VALUES ( 2016, 03, 17, 21, 04, '2016-03-17 21:04:19', '1234123412341234', 200.0, 'VISA','San Francisco', 'Nordstrom', 'asked for discounts', 'Approved', '763629', 'tomdavis', 'Fraudulent');
```

## Simple queries

We can now run SQL queries to look up all transactions for a given credit card (`cc_no`). 
The Transactions table is primarily write-oriented - it's the destination table for the streamed transactions and used for searches and we don't update the transactions once they have been written.

The table has a primary key so a typical query would look like this:
```
SQL> SELECT * FROM transactions WHERE cc_no='1234123412341234' and year=2016 and month=3 and day=9;

 TXN_ID       CC_NO       YEAR  MONTH  DAY       TXN_TIME       AMOUNT  CC_PROVIDER  COUNTRY  DATE_TEXT  HOUR  LOCATION  MERCHANT  MIN         NOTES          STATUS     TAG     USER_ID
 ------- ---------------- ----- ------ ---- ------------------- ------- ------------ -------- ---------- ----- --------- --------- ---- -------------------- -------- ---------- --------

 098765  1234123412341234 2016    3     9   2016-03-09 11:04:19   200       VISA      <null>    <null>    11    London   Ted Baker  4   pretty good clothing Approved Suspicious tomdavis
```
The roll-up tables can also be queried - for example transactions for each merchant by day use the dailytxns_bymerchant table.

The roll-up tables are empty at this point - they get populated using the Spark batch and streaming analytics jobs that we run later.



## Searching with SQL

Structured Query Language (SQL) allows you to search for data in the database tables that were created above.

For example, get all transactions for a specified card number on a specified day.
```
SQL> select t.txn_id, t.cc_no, t.merchant,ti.amount, ti.descr from transactions t inner join transaction_items ti on t.txn_id=ti.txn_id WHERE t.cc_no='1234123412341234' and t.txn_id=ti.txn_id order by t.txn_id;

 TXN_ID       CC_NO       MERCHANT  AMOUNT     DESCR
 ------- ---------------- --------- ------- -----------

 098765  1234123412341234 Ted Baker   125   Clothes
 098765  1234123412341234 Ted Baker   55    Shoes
 098765  1234123412341234 Ted Baker   25    Fragrance
 763629  1234123412341234 Nordstrom   125   Trousers
 763629  1234123412341234 Nordstrom   50    Dress-shirt
 763629  1234123412341234 Nordstrom   25    T-shirt
```

Get transactions by first 6 digits of cc_no and status.
```
SQL> SELECT * FROM transactions where cc_no like '123412%' and status='Rejected';;

 TXN_ID       CC_NO       YEAR  MONTH  DAY       TXN_TIME       AMOUNT  CC_PROVIDER  COUNTRY  DATE_TEXT  HOUR    LOCATION    MERCHANT  MIN         NOTES         STATUS     TAG     USER_ID
 ------- ---------------- ----- ------ ---- ------------------- ------- ------------ -------- ---------- ----- ------------- --------- ---- ------------------- -------- ---------- --------

 763629  1234123412341234 2016    3     17  2016-03-17 21:04:19   200       VISA      <null>    <null>    21   San Francisco Nordstrom  4   asked for discounts Rejected Fraudulent tomdavis
```

When we start generating some live data we'll be able to analyse up-to-date information.

These samples demonstrate that full, ad-hoc search on any of the transaction fields is possible including amounts, merchants etc.

Queries like this will be used to build the ReST interface. 

You can use SQL to explore the list of provided ReST queries here: https://github.com/simonambridge/NCFP/blob/master/SQL/SQL_Queries.md



## Step 4: Analyzing data using NuoDB Spark Analytics

NuoDB provides integration with Spark via the NuoDB JDBC driver to enable analysis of data in-place on the same cluster where the data is ingested and stored. Workloads can be isolated and there is no need to ETL the data.


### Streaming Analytics

The streaming analytics element of this application is made up of two parts:

* A transaction "producer" - a Scala/Akka app that generates random credit card transactions and then places those transactions onto a Kafka queue. 
* A transaction "consumer" - also written in Scala, is a Spark streaming job that 
(a) consumes the messages put on the Kafka queue, and then 
(b) parses those messages, evalutes the transaction status and then writes them to the NuoDB table `transactions`. 
It also generates rolling summary lines into the `txn_count_min` table every minute.

Streaming analytics code can be found under the directory `TransactionHandlers/producer` (pre-requisite: make sure you have run the SQL schema create script as described above to create the necessary tables).

Follow the Spark streaming installation and set up instructions here: https://github.com/simonambridge/NCFP/blob/master/TransactionHandlers/NCFP-Txn-Handlers-README.md



<sub>Acknowldegements: Based on the original project created with help from colleagues at DataStax.
<BR>NCFP now has a Node.js/D3 ReST interface replacing Java, enhanced producer/consumer codebase, new roll-up reports, real time charts, a new demo ReST UI, improved documentation, etc</sub>
