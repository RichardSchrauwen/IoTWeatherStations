# Mount for persistent data 
# -v /host/couchdb/data:/usr/local/var/lib/couchdb \

# Mount for custom config
# -v /host/couchdb/config:/usr/local/etc/couchdb/local.d \
# -v /host/couchdb/local.ini:/usr/local/etc/couchdb/local.ini \

# Get local.ini by copying from a running container
# docker cp <container-name>:/usr/local/etc/couchdb/local.ini /host/local.ini

# For easy restart
docker rm -f couchdb

# Run container
docker run \
  -p 5984:5984 \
  --name couchdb \
  -d couchdb

