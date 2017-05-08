#!/bin/bash

if [ $# -eq 0 ]; then
  echo 'Missing config file path'
  exit 1
fi

source $1

if [[ -z $APPIOT_REGISTRATION_TICKET ]]; then
  echo 'Missing registration ticket'
  exit 1
fi

if [ -z $APPIOT_COUCHDB_URL ]; then
  echo 'Missing CouchDB URL'
  exit 1
fi

java -jar appiot-owm.jar
