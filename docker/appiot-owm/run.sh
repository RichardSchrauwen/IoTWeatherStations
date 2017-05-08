#!/bin/bash

if [ $# -eq 0 ]; then
  echo 'Missing config file path'
  exit 1
fi

source $1

if [ -z $APPIOT_CONTAINER_NAME ]; then
  echo 'Missing container name'
  exit 1
fi

if [[ -z $APPIOT_REGISTRATION_TICKET ]]; then
  echo 'Missing registration ticket'
  exit 1
fi

if [ -z $APPIOT_COUCHDB_URL ]; then
  echo 'Missing CouchDB URL'
  exit 1
fi


sudo docker rm -f "appiot-owm-$APPIOT_CONTAINER_NAME" > /dev/null 2>&1
sudo docker run \
  -e APPIOT_REGISTRATION_TICKET="$APPIOT_REGISTRATION_TICKET" \
  -e APPIOT_COUCHDB_URL=$APPIOT_COUCHDB_URL \
  -e APPIOT_COUCHDB_USER=$APPIOT_COUCHDB_USER \
  -e APPIOT_COUCHDB_PASSWORD=$APPIOT_COUCHDB_PASSWORD \
  -d \
  --name "appiot-owm-$APPIOT_CONTAINER_NAME" appiot-owm

