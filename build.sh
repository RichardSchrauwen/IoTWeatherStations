#!/bin/bash

echo 'Building project'
mvn compile assembly:single
if [ $? -ne 0 ]; then
  echo 'Failed to build, exiting ...'
  exit 1
fi
echo 'Build complete'

echo 'Cleaning up'
rm appiot-owm.jar > /dev/null 2>&1
mv target/owmgateway-1.0-SNAPSHOT-jar-with-dependencies.jar appiot-owm.jar
cp appiot-owm.jar docker/appiot-owm/
rm -rf target > /dev/null 2>&1
echo 'Done'
