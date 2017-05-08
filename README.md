# AppIoT | Open Weather Map - 2.0

## AppIoT Setup

#### Gateway

###### Create Settings Category

* Create a new Settings Category called 'OWM Gateway Settings'.
* Toggle Hardware Settings to true.
* Add a Setting with the Name 'API-Key' and the Data-Type 'Text'.
* Leave the rest as is and Save.

###### Create Gateway Type

###### Register Gateway

#### Device

###### Create Settings Category

* Create a new Settings Category called 'OWM Device Settings'.
* Toggle Hardware Settings to true.
* Add a Setting with the Name 'CityID' and the Data-Type 'Text'.
* Leave the rest as is
* Add a Setting with the Name 'Enabled' and the Data-Type 'Boolean' with the Value 'True'.
* Leave the rest as is
* Add a Setting with the Name 'Interval' and the Data-Type 'Number' with the Value '60'.
* Leave the rest as is
* Save

###### Create Device Type

###### Register Device

## Server Setup

###### Build the OWM-gateway

``` bash
$ ./build.sh

``` 

###### Run CouchDB with Docker
```bash
# Change directory to docker/couchdb
cd docker/couchdb

# Run CouchDB. To configure the container see comments in the run.sh file.
./run.sh
```

###### Run without Docker

```bash
# Create a config file based on the config/example file.
# Run the run.sh script with your config file as the first argument

./run.sh config/your-config
```

###### Run with Docker

```bash
# Change directory to docker/appiot-owm
cd docker/appiot-owm

# Build the docker image
./build.sh

# Create a config file based on the docker/appiot-owm/config/example
# Run the run.sh script with your config file as the first argument
./run.sh config/your-docker-config
```
