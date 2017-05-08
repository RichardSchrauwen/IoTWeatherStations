# AppIoT | Open Weather Map - 2.0

## Build the OWM-gateway

``` bash
$ ./build.sh

``` 

## Run CouchDB with Docker
```bash
# Change directory to docker/couchdb
cd docker/couchdb

# Run CouchDB. To configure the container see comments in the run.sh file.
./run.sh
```

## Run 

```bash
# Create a config file based on the config/example file.
# Run the run.sh script with your config file as the first argument

./run.sh config/your-config
```

## Run with Docker

```bash
# Change directory to docker/appiot-owm
cd docker/appiot-owm

# Build the docker image
./build.sh

# Create a config file based on the docker/appiot-owm/config/example
# Run the run.sh script with your config file as the first argument
./run.sh config/your-docker-config
```
