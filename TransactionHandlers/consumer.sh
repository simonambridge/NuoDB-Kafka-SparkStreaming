IVY_PATH=$HOME/.ivy2 export IVY_PATH
spark-submit --jars $IVY_PATH/cache/com.typesafe/config/bundles/config-1.3.2.jar \
--packages org.apache.spark:spark-streaming-kafka_2.11:1.6.3 \
--class TransactionConsumer consumer/target/scala-2.11/consumer_2.11-0.1.jar \

