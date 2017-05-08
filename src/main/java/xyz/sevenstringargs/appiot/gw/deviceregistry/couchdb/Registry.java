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
import xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb.dto.DeviceRegistrationDTO;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class Registry implements DeviceRegistry {

    // DB --------------------------------------------------------------------------------------------------------------

    private static final String DB_NAME = "appiot";

    private String url;
    private String user;
    private String password;

    private CouchDbConnector conn;
    private Repository dbRegistry;

    // -----------------------------------------------------------------------------------------------------------------

    // In memory -------------------------------------------------------------------------------------------------------

    private HashMap<String, DeviceRegistration> registry = new HashMap<>();

    // -----------------------------------------------------------------------------------------------------------------

    // Other -----------------------------------------------------------------------------------------------------------

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private String gatewayId;

    // -----------------------------------------------------------------------------------------------------------------

    // Constructors ----------------------------------------------------------------------------------------------------

    public Registry(String gatewayId, String url) {
        this(gatewayId, url, null, null);
    }

    public Registry(String gatewayId, String url, String user, String password) {
        this.gatewayId = gatewayId;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    // -----------------------------------------------------------------------------------------------------------------

    // AppIoT Device Registry ------------------------------------------------------------------------------------------

    @Override
    public void init() throws DeviceRegistryException {
        connect();
        syncInMemory();
    }

    @Override
    public boolean isRegistered(String s) {
        return registry.containsKey(s);
    }

    @Override
    public void handleDeviceRegisterRequest(DeviceRegisterRequest deviceRegisterRequest) throws DeviceRegistryException {
        switch (deviceRegisterRequest.getOperation()) {
            case Operation.DELETE:
                removeRegistrationByEndpoint(deviceRegisterRequest.getEndpoint());
                break;
            case Operation.POST:
                create(convertRequest(deviceRegisterRequest));
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
        remove(s);
    }

    @Override
    public void updateRegistration(DeviceRegistration deviceRegistration) throws DeviceRegistryException {
        if (!registry.containsKey(deviceRegistration.getEndpoint())) {
            create(deviceRegistration);
            return;
        }
        update(deviceRegistration);
    }

    @Override
    public void clearRegistry() throws DeviceRegistryException {
        removeAll();
    }

    // -----------------------------------------------------------------------------------------------------------------

    // DB Methods ------------------------------------------------------------------------------------------------------

    private boolean useSecureConnection() {
        return user != null && password != null;
    }

    private void connect() throws DeviceRegistryException {
        try {
            StdHttpClient.Builder builder = new StdHttpClient.Builder().url(url);
            if (useSecureConnection()) {
                builder.password(password);
                builder.username(user);
            }

            HttpClient httpClient = builder.build();
            CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);

            conn = new StdCouchDbConnector(DB_NAME, dbInstance);
            conn.createDatabaseIfNotExists();
            dbRegistry = new Repository(conn);
        } catch (MalformedURLException e) {
            throw new DeviceRegistryException(e);
        }
    }

    private void dbCreate(DeviceRegistration deviceRegistration) {
        DeviceRegistrationDTO dto = DeviceRegistrationDTO.fromAppIoTModel(gatewayId, deviceRegistration);
        dbRegistry.add(dto);
    }

    private void dbRemoveAll() {
        Collection<DeviceRegistration> devices = registry.values();
        for (DeviceRegistration d : devices) {
            DeviceRegistrationDTO dto = dbRegistry.get(d.getEndpoint());
            dbRegistry.remove(dto);
        }
    }

    private void dbUpdate(DeviceRegistration deviceRegistration) {
        DeviceRegistrationDTO oldDto = dbRegistry.get(deviceRegistration.getEndpoint());
        DeviceRegistrationDTO dto = DeviceRegistrationDTO.fromAppIoTModel(gatewayId, deviceRegistration);
        dto.setRevision(oldDto.getRevision());
        dto.setId(oldDto.getId());
        dbRegistry.update(dto);
    }

    private void dbRemove(String s) {
        DeviceRegistrationDTO dto = dbRegistry.get(s);
        dbRegistry.remove(dto);
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Wrapper Methods -------------------------------------------------------------------------------------------------

    private void syncInMemory() {
        if (dbRegistry == null) {
            logger.warning("Failed to sync in-memory registry with persistent store");
            return;
        }

        dbRegistry.getAll().
                stream().
                filter(d -> d.getGatewayId().equals(gatewayId)).
                forEach(d -> registry.put(d.getEndpoint(), DeviceRegistrationDTO.toAppIoTModel(d)));
    }

    private void remove(String s) {
        dbRemove(s);
        registry.remove(s);
    }

    private void removeAll() {
        dbRemoveAll();
        registry.clear();
    }

    private void update(DeviceRegistration deviceRegistration) {
        dbUpdate(deviceRegistration);
        registry.put(deviceRegistration.getEndpoint(), deviceRegistration);
    }

    private void create(DeviceRegistration deviceRegistration) {
        dbCreate(deviceRegistration);
        registry.put(deviceRegistration.getEndpoint(), deviceRegistration);
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Other -----------------------------------------------------------------------------------------------------------

    private DeviceRegistration convertRequest(DeviceRegisterRequest deviceRegisterRequest) {
        DeviceRegistration devReg = new DeviceRegistration();
        devReg.setEndpoint(deviceRegisterRequest.getEndpoint());
        devReg.setInternal(deviceRegisterRequest.isInternal());
        devReg.setName(deviceRegisterRequest.getName());
        devReg.setResourceLinks(deviceRegisterRequest.getResourceLinks());
        devReg.setSettingCategories(deviceRegisterRequest.getSettingCategories());

        return devReg;
    }

    // -----------------------------------------------------------------------------------------------------------------
}
