package xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

public class DeviceRegistryRepository extends CouchDbRepositorySupport<DeviceRegistrationDTO> {
    public DeviceRegistryRepository(CouchDbConnector conn){
        super(DeviceRegistrationDTO.class, conn);
    }
}
