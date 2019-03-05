# IoT Application based on Open Weather Map (OWM) API

This is an AppIoT soft-gateway that will poll Open Weather Map based on its registered devices. The gateway uses CouchDB as backing storage for its device registry and the project includes instructions for running the database and gateway in docker containers. Multiple gateways can use the same CouchDB instance as long as the gateways are registered in the same device network.

#### TODO
* Remove CouchDB dependency and go for cloud storage in the IoT platform itself
* Insert new API keys from OWM

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
