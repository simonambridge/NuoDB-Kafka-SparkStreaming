## Install and Use Apache Spark

### Download and Install

Spark downloads and more information are available here: https://spark.apache.org/downloads.html
```
$ curl http://apache.mirror.anlx.net/spark/spark-2.4.4/spark-2.4.4-bin-hadoop2.7.tgz -o spark-2.4.4-bin-hadoop2.7.tgz
$ tar -zxvf spark-2.4.4-bin-hadoop2.7.tgz
$ sudo mv spark-2.4.4-bin-hadoop2.7 /opt
```

```
$ export SPARK_HOME=/opt/spark-2.4.4-bin-hadoop2.7
```


### Add NuoDB JDBC To Spark Classpath


Create a working copy of the Spark defaults file: 
```
$ cp $SPARK_HOME/conf/spark-defaults.conf.template $SPARK_HOME/conf/spark-defaults.conf
```

Edit the file and add the external JDBC jar dependencies:
```
$ vi $SPARK_HOME/conf/spark-defaults.conf
```

```
# Default system properties included when running spark-submit.
# This is useful for setting default environmental settings.

# Example:
# spark.master                     spark://master:7077
# spark.eventLog.enabled           true
# spark.eventLog.dir               hdfs://namenode:8021/directory
# spark.serializer                 org.apache.spark.serializer.KryoSerializer
# spark.driver.memory              5g
# spark.executor.extraJavaOptions  -XX:+PrintGCDetails -Dkey=value -Dnumbers="one two three"
```

Add the following:
```
spark.driver.extraClassPath = /opt/nuodb/jar/nuodbjdbc.jar
spark.executor.extraClassPath = /opt/nuodb/jar/nuodbjdbc.jar
```

### Update user environment and add to $HOME/.bashrc

Update PATH - it should include $KAFKA_HOME/bin, $JAVA_HOME/bin, $SPARK_HOME/bin:
```
$ export PATH=$KAFKA_HOME/bin:$JAVA_HOME/bin:$SPARK_HOME/bin:$PATH
```
Update CLASSPATH it should include $KAFKA_HOME/lib, $SPARK_HOME/jars, $JAVA_HOME/lib and the NuoDB JDBC driver:
```
$ export CLASSPATH=$KAFKA_HOME/lib:$SPARK_HOME/jars:$JAVA_HOME/lib:/opt/nuodb/jar
```



### Start The Spark Master
```
$ sudo $SPARK_HOME/sbin/start-master.sh

starting org.apache.spark.deploy.master.Master, logging to /opt/spark-2.4.4-bin-hadoop2.7/logs/spark-root-org.apache.spark.deploy.master.Master-1-ip-172-31-0-207.eu-west-2.compute.internal.out
```

### Check The Spark UI

http://localhost:8080/
or 
http://35.177.227.213:8080/

Spark Master at spark://ip-172-31-0-207.eu-west-2.compute.internal:7077
URL: spark://ip-172-31-0-207.eu-west-2.compute.internal:7077
Alive Workers: 0
Cores in use: 0 Total, 0 Used
Memory in use: 0.0 B Total, 0.0 B Used
Applications: 0 Running, 0 Completed
Drivers: 0 Running, 0 Completed
Status: ALIVE


### Start The Spark 2.4 Shell
```
$ $SPARK_HOME/bin/spark-shell --driver-class-path /opt/nuodb/jar/nuodbjdbc.jar
19/12/19 22:16:23 WARN NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
Using Spark's default log4j profile: org/apache/spark/log4j-defaults.properties
Setting default log level to "WARN".
To adjust logging level use sc.setLogLevel(newLevel). For SparkR, use setLogLevel(newLevel).
Spark context Web UI available at http://ip-172-31-0-207.eu-west-2.compute.internal:4040
Spark context available as 'sc' (master = local[*], app id = local-1576793794563).
Spark session available as 'spark'.
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.4.4
      /_/

Using Scala version 2.11.12 (OpenJDK 64-Bit Server VM, Java 1.8.0_222)
Type in expressions to have them evaluated.
Type :help for more information.

scala>
```

### Load Classes

(Hint: to paste text in the Spark shell you must start paste mode with :paste <enter> and exit paste mode by pressing ^D)


```
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SaveMode}

import java.util.Properties;
import java.io._

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
```

### Set Up Some Variables etc

```
val jdbcHostname = "35.177.227.213"
val jdbcPort = 48004
val jdbcDatabase = "hockey"
val jdbcUsername = "dba"
val jdbcPassword = "dba"
val jdbcSchema   = "user"
```

### Create the JDBC URL without passing in the user and password parameters.

```
val jdbcUrl = s"jdbc:com.nuodb://${jdbcHostname}:${jdbcPort}/${jdbcDatabase}"

// Create a Properties() object to hold the parameters.
val connProperties = new Properties()
connProperties.put("user", s"${jdbcUsername}")
connProperties.put("password", s"${jdbcPassword}")
connProperties.put("schema", s"${jdbcSchema}")
```


###  Connect to NuoDB Via JDBC & Create A Dataframe From  The Players Table  - Format 1

Pass the JDBC parameters explicitly:

```
scala> val players = spark.read
  .format("jdbc")
  .option("url", "jdbc:com.nuodb://localhost:48004/hockey?user=dba&password=dba&schema=user")
  .option("dbtable", "players")
  .load()
```

Output:
```
players: org.apache.spark.sql.DataFrame = [PLAYERID: string, FIRSTNAME: string ... 12 more fields]
```

Simplified syntax and use the connProperties:

```
scala> val players = spark.read.jdbc(jdbcUrl, "players", connProperties)
```
Output
```
players: org.apache.spark.sql.DataFrame = [PLAYERID: string, FIRSTNAME: string ... 12 more fields]
```

###  View Dataframe Contents

Show dataframe contents 
```
scala> players.show(5)
+---------+---------+----------+------+------+--------+-------+--------+---------+--------+--------+------------+----------+------------+
| PLAYERID|FIRSTNAME|  LASTNAME|HEIGHT|WEIGHT|FIRSTNHL|LASTNHL|POSITION|BIRTHYEAR|BIRTHMON|BIRTHDAY|BIRTHCOUNTRY|BIRTHSTATE|   BIRTHCITY|
+---------+---------+----------+------+------+--------+-------+--------+---------+--------+--------+------------+----------+------------+
|aaltoan01|    Antti|     Aalto|    73|   210|    1997|   2000|       C|     1975|       3|       4|     Finland|         0|Lappeenranta|
|abbeybr01|    Bruce|     Abbey|    73|   185|       0|      0|       D|     1951|       8|      18|      Canada|        ON|     Toronto|
|abbotge01|   George|    Abbott|    67|   153|    1943|   1943|       G|     1911|       8|       3|      Canada|        ON|    Synenham|
|abbotre01|      Reg|    Abbott|    71|   164|    1952|   1952|       C|     1930|       2|       4|      Canada|        MB|    Winnipeg|
|abdelju01|   Justin|Abdelkader|    73|   195|    2007|   2011|       L|     1987|       2|      25|         USA|        MI|    Muskegon|
+---------+---------+----------+------+------+--------+-------+--------+---------+--------+--------+------------+----------+------------+
only showing top 5 rows
```

###  Register A SparkSQL Temp Table
```
scala> players.registerTempTable("playersTable")
warning: there was one deprecation warning; re-run with -deprecation for details
```

###  Select Rows From The Temp Table & Write Them Directly To NuoDB

This will create a NuoDB database table called playerstable and adds 10 records selected from the Spark temp table called playersTable.

```
scala> spark.sql("select * from playersTable limit 10")
.write
.mode(SaveMode.Append)
.jdbc(jdbcUrl, "playerstable", connProperties)
```

>> Warning message (*** check isolation levels)
```
19/12/19 22:56:52 WARN JdbcUtils: Requested isolation level 1 is not supported; falling back to default isolation level 8
```

```
SQL> select count(*) from playerstable;

 COUNT  
 ------ 

   10   
```

###  Write The TempTable To NuoDB As PLAYERSTABLE2

Use the write command to create. new table and save the contents of the Sparl temp tavblr playersTable:

```
scala> spark.table("playersTable")
.write
.jdbc(jdbcUrl, "playerstable2", connProperties)


[Stage 1:> (0 + 1) / 1]19/12/19 23:35:51 WARN JdbcUtils: Requested isolation level 1 is not supported; falling back to default isolation level 8
```

Alternatively:

```
scala> val players_table = spark.write.jdbc(jdbcUrl, "players", connProperties)
```

Confirm that the records are written to NuoDB
```
SQL> select count(*) from playerstable2;

 COUNT  
 ------ 

  7520  
```


### WRrite A DataFrame To NuoDB

Write the contents of the players dtaframe to NuoDB - overwrite the table playerstable if it exists:

```
scala> players.write.format("jdbc")
  .option("url", "jdbc:com.nuodb://localhost:48004/hockey?user=dba&password=dba&schema=user")
  .option("dbtable", "playerstable")
.mode(SaveMode.Overwrite)
  .save()
```
Output
```
19/12/19 23:47:43 WARN JdbcUtils: Requested isolation level 1 is not supported; falling back to default isolation level 8
```


## Install The Hadoop Native libraries

Follow these steps to avoid "WARN NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable" warning

```
$ wget http://apache.mirror.anlx.net/hadoop/common/hadoop-2.7.7/hadoop-2.7.7.tar.gz
$ gunzip hadoop-2.7.7.tar.gz
$ cd /opt
$ sudo tar xvf <path>/hadoop-2.7.7.tar

$ export LD_LIBRARY_PATH=/opt/hadoop-2.7.7/lib
```

