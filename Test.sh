#!/bin/bash

#konfigurai run aplikasi
RABBITMQ_SERVER="localhost"
RABBITMQ_PORT="5672"
SERVER_QUEUE_NAME="TugasRabbitMQServerQueue_nim_13512501"

#konfigurasi testing
NUM_CLIENTS=4
TERMINAL="gnome-terminal"

#lokasi jar
CLIENT_JAR=TugasRabbitMQClient/out/artifacts/TugasRabbitMQClient_jar/TugasRabbitMQClient.jar
SERVER_JAR=TugasRabbitMQServer/out/artifacts/TugasRabbitMQServer_jar/TugasRabbitMQServer.jar

for i in `seq 1 $NUM_CLIENTS`;
do
	echo "running client $i"
	$TERMINAL -e "java -jar $CLIENT_JAR $RABBITMQ_SERVER $RABBITMQ_PORT $SERVER_QUEUE_NAME"
done
echo "running server"
java -jar $SERVER_JAR $RABBITMQ_SERVER $RABBITMQ_PORT $SERVER_QUEUE_NAME
