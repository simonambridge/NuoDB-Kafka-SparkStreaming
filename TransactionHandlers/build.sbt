val globalSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.0"
)

val akkaVersion = "2.5.9"
val sparkVersion = "2.4.4"
val kafkaVersion = "2.4.0"
val configVersion = "1.3.2"
val sskVersion = "1.6.3"

lazy val producer = (project in file("producer"))
  .settings(name := "producer")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= producerDeps)

lazy val consumer = (project in file("consumer"))
  .settings(name := "consumer")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= consumerDeps)


lazy val producerDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.apache.kafka"  %% "kafka"      % kafkaVersion
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

lazy val consumerDeps = Seq(
  "com.typesafe.akka" %% "akka-actor"            % akkaVersion,
//  "org.apache.kafka"  %% "kafka"                 % kafkaVersion,
//  "org.scala-lang"    % "scala-reflect"          % "2.11.0",
  "org.apache.spark"  %% "spark-streaming-kafka" % sskVersion,
  "org.apache.spark"  %% "spark-sql"             % sparkVersion,
  "org.apache.spark"  %% "spark-streaming"       % sparkVersion,
  "com.typesafe"      %  "config"                % configVersion,
)

