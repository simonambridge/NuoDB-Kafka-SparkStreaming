### Download Apache Kafka

Kafka can be downloaded from this URL: [http://kafka.apache.org/downloads.html](http://kafka.apache.org/downloads.html)

Get the Scala 2.12 release (it should match the Spark Scala version)

Download and install the binary version for Scala 2.12 - you can use wget or curl to download to the server e.g:
```
$ curl http://apache.mirror.anlx.net/kafka/2.4.0/kafka_2.12-2.4.0.tgz  -o kafka_2.12-2.4.0.tgz
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 59.3M  100 59.3M    0     0  32.7M      0  0:00:01  0:00:01 --:--:-- 32.7M```
```

### Install Apache Kafka

Extract the file - it will create a folder/directory that can then move to a location of your choice.

```
$ gunzip kafka_2.12-2.4.0.tgz
```
Now extract and create the directory ```kafka_2.12-2.4.0```

```
$ tar xvf kafka_2.12-2.4.0.tgz
```

Move the extracted Kafka directory tree to your preferred location, e.g.:

```
$ sudo mv kafka_2.12-2.4.0 /opt
$ export KAFKA_HOME=/opt/kafka_2.12-2.4.0
```
```
export KAFKA_HOME=/opt/kafka_2.12-2.4.0
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.222.b10-0.amzn2.0.1.x86_64/jre
export PATH=$KAFKA_HOME/bin:$JAVA_HOME/bin:$PATH
export CLASSPATH=$KAFKA_HOME/lib
```

### Start ZooKeeper 

a. Start a local copy of zookeeper (in its own terminal or use nohup).

```
$ sudo $KAFKA_HOME/bin/zookeeper-server-start.sh $KAFKA_HOME/config/zookeeper.properties
```
 
For example, to start keep zookeeper and keep it running:
```
$ sudo -b nohup $KAFKA_HOME/bin/zookeeper-server-start.sh config/zookeeper.properties > $KAFKA_HOME/logs/nohupz.out 2>&1
```

Test Zookeeper is alive:

$ telnet localhost 2181
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
^C

And
```
$ sudo netstat -tulpn | grep 2181
tcp6       0      0 :::2181                 :::*                    LISTEN      4082/java
```


### Start Kafka 

Start a local copy of Kafka (in its own terminal or use nohup):

```
$ sudo $KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties
```

For example (using a different output file to the one created by the Zookeeper process):
```
$ sudo -b nohup $KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties > $KAFKA_HOME/logs/nohupk.out 2>&1 
```


### Prepare a message topic for use.

Create the topic we will use for the lab:

For example:
```
$ $KAFKA_HOME/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic NewTransactions
Created topic "NewTransactions"
```

Validate the topic was created:

```
$ $KAFKA_HOME/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
NewTransactions
```

Test the Producer & Consumer on Topic NewTransactions

Start the CLI producer and then type a few messages into the console to send to the server:

$ $KAFKA_HOME/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic NewTransactions
>This is a test message
>End of message
>
>^C
$

Start the CLI consumer to dump out messages to standard output:

$ $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic NewTransactions --from-beginning
This is a test message
End of message

^CProcessed a total of 3 messages
$

If you use a different terminal for the producer and consumer you can type messages into the producer terminal and see them appear in the consumer terminal.

For more go [here](http://kafka.apache.org/quickstart)

You can now leave Kafka and Zookeeper running while you set up the streaming part of the demo.



### Some more useful Kafka commands
	
> Kafka does not automatically remove messages from the queue after they have been read. This allows for the possibility of recovery in the event that the consumer dies

By default Kafka will retain messages in the queue for 7 days - to change message retention to e.g. 1 hour (360000 milliseconds) 
```
$ $KAFKA_HOME/bin/kafka-configs.sh --zookeeper localhost:2181 --alter --entity-type topics --entity-name test_topic --add-config retention.ms=360000
```

To allow larger messages to be used, change max.message.bytes - e.g. to change the max message size to 128K:
```
$ $KAFKA_HOME/bin/kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --entity-name NewTransactions --alter --add-config max.message.bytes=128000
Completed Updating config for entity: topic 'NewTransactions'.
```

Now display the topic configuration details:
```
$ $KAFKA_HOME/bin/kafka-configs.sh --zookeeper localhost:2181 --describe --entity-name NewTransactions --entity-type topics
Configs for topic 'NewTransactions' are retention.ms=360000,max.message.bytes=128000
```

Describe the 'NewTransactions' topic:
```
$ $KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic NewTransactions
Topic: NewTransactions  PartitionCount: 1 ReplicationFactor: 1  Configs: segment.bytes=1073741824,max.message.bytes=128000
  Topic: NewTransactions  Partition: 0  Leader: 0 Replicas: 0 Isr: 0
```

Use the following command to completely delete the topic. (Note: The server.properties file must contain `delete.topic.enable=true` for this to work):
```
$ $KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic NewTransactions
```

Show all of the messages in a topic from the beginning:
```
$ $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic NewTransactions --from-beginning
This is a test message
End of message
```

Leave it running when the producer is running you'll see new transactions appear like this:
```
$ $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic NewTransactions --from-beginning

0009000009814738;0009;2017-12-05 21:15:48.065;3bc27951-d9de-456e-88d0-8b1f185a7f79;Rite Aid;;BD;Item_14024->465.98,Item_45596->851.84,Item_289->661.89;1979.71;36
6135000096830767;6135;2017-12-05 21:15:48.072;1886c216-e2da-4f04-ad62-c84c19e40310;McDonald's;;IN;Item_63884->333.87;333.87;50
9449000021756283;9449;2017-12-05 21:15:48.558;6075f09f-6621-470f-8726-531150aab121;Wal-Mart Stores;;BD;Item_70557->177.29,Item_68655->701.42,Item_61007->818.01;1696.73;58
8708000096315232;8708;2017-12-05 21:15:48.559;b713cd53-4a82-43da-a77b-4e4b77bbfe92;Starbucks;;EG;Item_29895->171.05,Item_8024->17.93,Item_63002->647.52,Item_42622->536.01;1372.51;48
0885000026563684;0885;2017-12-05 21:15:48.559;95499955-dcd0-45e1-a1ec-4af948017579;Wal-Mart Stores;;AU;Item_98422->192.47,Item_96991->340.10;532.57;34
0660000006438110;0660;2017-12-05 21:15:48.56;9d67f0f2-ddcd-40de-83ee-9b8168dec5e7;Gap;MD;US;Item_22347->760.93,Item_17992->176.15,Item_17441->459.07;1396.14;56
```
