# AppIoT | Open Weather Map - 2.0

## AppIoT Setup

This is an AppIoT soft-gateway will poll Open Weather Map based on its registered devices. The gateway uses CouchDB as backing storage for its device registry and is made to run with Docker. Multiple gateways can use the same CouchDB instance as long as the gateways are registered in the same device network.

#### Gateway

###### Create Settings Category

* Create a new Settings Category called 'OWM Gateway Settings'.
* Toggle Hardware Settings to true.
* Add a Setting with the Name 'API-Key' and the Data-Type 'Text'.
* Leave the rest as is and Save.

###### Create Gateway Type

* Create a Gateway Type with the Name 'OWM-Gateway'
* Add the previously created Setting Category 'OWM Gateway Settings' to the type.
* Save

###### Register Gateway

Get an Open Weather Map API key if you don't have one.

* Register a gateway of the 'OWM-Gateway' type.
* Enter your Open Weather Map API-key in the API-Key settings field
* Save

------

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

* Create a new Device Type with the Name 'OWM-Device'
* Set Device Standard to LWM2M
* Discover at registration set to false
* Add four smart objects: Barometer(3315), Humidity(3304), Location(3336), Temperature(3303)
* Add the newly created Settings Category 'OWM Device Settings'
* Save

###### Register Device

* Register a new device based on the 'OWM-Device'
* Enter any name
* Enter a unique Endpoint
* Select the 'OWM-Device' Device Type
* Select your registered Gateway
* Add the 'OWM-Device Settings' and fill in the CityID of the city you want to poll for data
* Save

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
