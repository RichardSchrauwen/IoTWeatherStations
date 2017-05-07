package xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb;

import com.ericsson.appiot.gateway.deviceregistry.DeviceRegistration;
import com.ericsson.appiot.gateway.deviceregistry.DeviceRegistry;
import com.ericsson.appiot.gateway.deviceregistry.DeviceRegistryException;
import com.ericsson.appiot.gateway.dto.DeviceRegisterRequest;
import com.ericsson.appiot.gateway.dto.Operation;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class CouchDBRegistry implements DeviceRegistry {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private String gatewayId;
    private String url;
    private String db;
    private String user;
    private String password;

    private HashMap<String, DeviceRegistration> registry;

    private CouchDbConnector conn;
    private DeviceRegistryRepository dbRegistry;

    public CouchDBRegistry(String gatewayId, String url, String db) throws MalformedURLException {
        this(gatewayId, url, db, null, null);
    }

    public CouchDBRegistry(String gatewayId, String url, String db, String user, String password) throws MalformedURLException {
        this.gatewayId = gatewayId;
        this.url = url;
        this.db = db;
        this.user = user;
        this.password = password;
        this.registry = new HashMap<>();
    }

    private boolean useSecureConnection() {
        return user != null && password != null;
    }

    @Override
    public void init() throws DeviceRegistryException {
        HttpClient httpClient = null;
        try {
            StdHttpClient.Builder builder = new StdHttpClient.Builder().url(url);
            if (useSecureConnection()) {
                builder.password(password);
                builder.username(user);
            }

            httpClient = builder.build();
            CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);

            conn = new StdCouchDbConnector(db, dbInstance);
            conn.createDatabaseIfNotExists();
            dbRegistry = new DeviceRegistryRepository(conn);
            dbRegistry.getAll().
                    stream().
                    filter(d -> d.getGatewayId().equals(gatewayId)).
                    forEach(d -> registry.put(d.getEndpoint(), DeviceRegistrationDTO.toAppIoTModel(d)));
        } catch (MalformedURLException e) {
            throw new DeviceRegistryException(e);
        }
    }

    @Override
    public boolean isRegistered(String s) {
        return registry.containsKey(s);
    }

    private DeviceRegistration convertRequest(DeviceRegisterRequest deviceRegisterRequest){
        DeviceRegistration devReg = new DeviceRegistration();
        devReg.setEndpoint(deviceRegisterRequest.getEndpoint());
        devReg.setInternal(deviceRegisterRequest.isInternal());
        devReg.setName(deviceRegisterRequest.getName());
        devReg.setResourceLinks(deviceRegisterRequest.getResourceLinks());
        devReg.setSettingCategories(deviceRegisterRequest.getSettingCategories());

        return devReg;
    }

    @Override
    public void handleDeviceRegisterRequest(DeviceRegisterRequest deviceRegisterRequest) throws DeviceRegistryException {
        switch(deviceRegisterRequest.getOperation()){
            case Operation.DELETE:
                removeRegistrationByEndpoint(deviceRegisterRequest.getEndpoint());
                break;
            case Operation.POST:
                DeviceRegistration devReg = convertRequest(deviceRegisterRequest);
                DeviceRegistrationDTO dto = DeviceRegistrationDTO.fromAppIoTModel(gatewayId, devReg);
                registry.put(devReg.getEndpoint(), devReg);
                dbRegistry.add(dto);
                break;
            case Operation.PUT:
                updateRegistration(convertRequest(deviceRegisterRequest));
                break;
        }
    }

    @Override
    public List<DeviceRegistration> getRegistrations() {
        return new ArrayList<>(registry.values());
    }

    @Override
    public DeviceRegistration getRegistrationByEndpoint(String s) {
        return registry.get(s);
    }

    @Override
    public void removeRegistrationByEndpoint(String s) throws DeviceRegistryException {
        registry.remove(s);
        DeviceRegistrationDTO dto = dbRegistry.get(s);
        dbRegistry.remove(dto);
    }

    private DeviceRegisterRequest convertRegistration(DeviceRegistration reg) {
        DeviceRegisterRequest req = new DeviceRegisterRequest();
        req.setDataCollectorId(gatewayId);
        req.setEndpoint(req.getEndpoint());
        req.setInternal(reg.isInternal());
        req.setName(req.getName());
        req.setResourceLinks(reg.getResourceLinks());
        req.setSettingCategories(reg.getSettingCategories());

        return req;
    }

    @Override
    public void updateRegistration(DeviceRegistration deviceRegistration) throws DeviceRegistryException {
        if (!registry.containsKey(deviceRegistration.getEndpoint())){
            DeviceRegistration devReg = deviceRegistration;
            DeviceRegistrationDTO dto = DeviceRegistrationDTO.fromAppIoTModel(gatewayId, devReg);
            registry.put(devReg.getEndpoint(), devReg);
            dbRegistry.add(dto);
        } else {
            registry.put(deviceRegistration.getEndpoint(), deviceRegistration);
            DeviceRegistrationDTO oldDto = dbRegistry.get(deviceRegistration.getEndpoint());
            DeviceRegistrationDTO dto = DeviceRegistrationDTO.fromAppIoTModel(gatewayId, deviceRegistration);
            dto.setRevision(oldDto.getRevision());
            dto.setId(oldDto.getId());
            dbRegistry.update(dto);
        }
    }

    @Override
    public void clearRegistry() throws DeviceRegistryException {
        Collection<DeviceRegistration> devices = registry.values();
        registry.clear();

        for (DeviceRegistration d : devices){
            DeviceRegistrationDTO dto = dbRegistry.get(d.getEndpoint());
            dbRegistry.remove(dto);
        }
    }
}
