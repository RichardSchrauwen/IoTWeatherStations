package xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb.dto.DeviceRegistrationDTO;

public class Repository extends CouchDbRepositorySupport<DeviceRegistrationDTO> {
    public Repository(CouchDbConnector conn) {
        super(DeviceRegistrationDTO.class, conn);
    }
}
