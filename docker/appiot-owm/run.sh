if [ $# -eq 0 ]; then
  echo 'Missing name of container'
  exit 1
fi

sudo docker rm -f "owm-appiot-$1"
sudo docker run \
  -it \
  --rm \
  -e APPIOT_REGISTRATION_TICKET='<REGISTRATION-TICKET>' \
  -e APPIOT_COUCHDB_URL='<COUCHDB-URL>' \
  -e APPIOT_COUCHDB_USER='<COUCHDB-USER>' \
  -e APPIOT_COUCHDB_PASSWORD='<COUCHDB-PASSWORD' \
  --name "owm-appiot-$1" appiot-owm

