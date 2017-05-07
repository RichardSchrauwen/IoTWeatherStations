package xyz.sevenstringargs.appiot.owmgw;

import com.ericsson.appiot.gateway.AppIoTGateway;
import com.ericsson.appiot.gateway.GatewayException;
import com.ericsson.appiot.gateway.device.Device;
import com.ericsson.appiot.gateway.device.DeviceAppIoTListener;
import com.ericsson.appiot.gateway.device.DeviceManager;
import com.ericsson.appiot.gateway.dto.DeviceRegisterRequest;
import com.ericsson.appiot.gateway.dto.Operation;
import com.ericsson.appiot.gateway.dto.SettingCategory;
import net.aksingh.owmjapis.OpenWeatherMap;
import xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb.CouchDBRegistry;

import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Logger;

public class Gateway extends DeviceAppIoTListener {

    private static final String ENV_KEY_REGISTRATION_TICKET = "APPIOT_REGISTRATION_TICKET";

    private static final String ENV_KEY_COUCHDB_URL = "APPIOT_COUCHDB_URL";
    private static final String ENV_KEY_COUCHDB_USER = "APPIOT_COUCHDB_USER";
    private static final String ENV_KEY_COUCHDB_PASSWORD = "APPIOT_COUCHDB_PASSWORD";

    private static final String OWM_GATEWAY_SETTINGS_NAME = "OWM Gateway Settings";
    private static final String OWM_GATEWAY_API_KEY_NAME = "API-Key";

    public static void main(String[] args) throws InterruptedException, MalformedURLException, GatewayException {
        Gateway gateway = new Gateway(new DeviceManager());
        gateway.run();
    }

    // OWM Gateway -----------------------------------------------------------------------------------------------------

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private AppIoTGateway appiotGateway;
    private Map<String, CurrentWeatherDevice> pollerRegistry;
    private String apiKey;

    public Gateway(DeviceManager deviceManager) throws MalformedURLException, GatewayException {
        super(deviceManager);
        appiotGateway = new AppIoTGateway(this);
        pollerRegistry = new HashMap<>();
        Home home = new Home(System.getenv(ENV_KEY_REGISTRATION_TICKET));
        appiotGateway.setHomeDirectory(home);


        String dbUrl = System.getenv(ENV_KEY_COUCHDB_URL);
        String dbUser = System.getenv(ENV_KEY_COUCHDB_USER);
        String dbPassword = System.getenv(ENV_KEY_COUCHDB_PASSWORD);

        logger.info(String.format("DB connection info | URL: %s, User: %s", dbUrl, dbUser));
        if (dbUser != null && dbUser.length() > 0 && dbPassword != null && dbPassword.length() > 0){
            appiotGateway.setDeviceRegistry(new CouchDBRegistry(home.getRegistrationTicket().getDataCollectorId(), dbUrl, "appiot", dbUser, dbPassword));
        } else {
            appiotGateway.setDeviceRegistry(new CouchDBRegistry(home.getRegistrationTicket().getDataCollectorId(), dbUrl, "appiot"));
        }
    }

    private void run() throws InterruptedException {
        appiotGateway.start();

        logger.info("Starting AppIoT Gateway");
        SettingCategory settings = appiotGateway.getProperties().getSettingCategory(OWM_GATEWAY_SETTINGS_NAME);
        if (settings != null){
            apiKey = settings.getSettingValue(OWM_GATEWAY_API_KEY_NAME);
        }

        Collection<Device> devices = getDeviceManager().getDevices();
        logger.info(String.format("Starting %d pollers", devices.size()));
        for (Device d : devices) {
            d.addDeviceObservationListener(this);
            CurrentWeatherDevice poller = new CurrentWeatherDevice(createOWMClient(apiKey), d);
            pollerRegistry.put(d.getEndpoint(), poller);
            poller.start();
            logger.info(String.format("Poller %s started", d.getEndpoint()));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    // AppIoT Listener Override ----------------------------------------------------------------------------------------

    @Override
    public void onDeviceRegisterRequest(String correlationId, String endpoint, DeviceRegisterRequest deviceRegisterRequest) {
        super.onDeviceRegisterRequest(correlationId, endpoint, deviceRegisterRequest);
        Device device = getDeviceManager().getDevice(endpoint);

        CurrentWeatherDevice poller;
        switch (deviceRegisterRequest.getOperation()) {
            case Operation.DELETE:
                logger.info(String.format("Deleting poller %s", endpoint));
                poller = pollerRegistry.remove(endpoint);
                if (poller != null) {
                    poller.stop();
                }
                break;
            case Operation.PUT:
                logger.info(String.format("Updating poller %s", endpoint));
                poller = pollerRegistry.get(endpoint);
                if (poller != null) {
                    poller.update(device);
                } else {
                    poller = new CurrentWeatherDevice(createOWMClient(apiKey), device);
                    pollerRegistry.put(endpoint, poller);
                    poller.start();
                }
                break;
            case Operation.POST:
                logger.info(String.format("Starting a new poller %s", endpoint));
                poller = new CurrentWeatherDevice(createOWMClient(apiKey), device);
                pollerRegistry.put(endpoint, poller);
                poller.start();
                break;
        }
    }

    @Override
    public void onGatewayUpdateSettingsRequest(List<SettingCategory> settings) {
        super.onGatewayUpdateSettingsRequest(settings);

        Optional<SettingCategory> owmSettingsOpt = settings.stream().filter(s -> s.getName().equals(OWM_GATEWAY_SETTINGS_NAME)).findFirst();
        if (!owmSettingsOpt.isPresent()) {
            return;
        }

        String newKey = owmSettingsOpt.get().getSettingValue(OWM_GATEWAY_API_KEY_NAME);
        if (newKey.equals(apiKey)) {
            return;
        }

        logger.info(String.format("Updating api key from %s to %s", apiKey, newKey));
        apiKey = newKey;
        for (CurrentWeatherDevice d : pollerRegistry.values()) {
            d.update(createOWMClient(apiKey));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    private OpenWeatherMap createOWMClient(String apiKey) {
        OpenWeatherMap client = new OpenWeatherMap(apiKey);
        client.setUnits(OpenWeatherMap.Units.METRIC);
        return client;
    }
}
