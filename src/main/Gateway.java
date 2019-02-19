package com.ericcson.appiot.examples.gw.owm;

import com.ericsson.appiot.gateway.AppIoTGateway;
import com.ericsson.appiot.gateway.GatewayException;
import com.ericsson.appiot.gateway.device.Device;
import com.ericsson.appiot.gateway.device.DeviceAppIoTListener;
import com.ericsson.appiot.gateway.device.DeviceManager;
import com.ericsson.appiot.gateway.deviceregistry.DeviceRegistry;
import com.ericsson.appiot.gateway.dto.DeviceRegisterRequest;
import com.ericsson.appiot.gateway.dto.Operation;
import com.ericsson.appiot.gateway.dto.SettingCategory;
import net.aksingh.owmjapis.OpenWeatherMap;
import com.ericcson.appiot.examples.gw.deviceregistry.couchdb.Registry;

import java.util.*;
import java.util.logging.Logger;

public class Gateway extends DeviceAppIoTListener {

    // Start Up / Settings Plumbing ------------------------------------------------------------------------------------

    public static void main(String[] args) {
        Logger mainLogger = Logger.getAnonymousLogger();

        String ticket = System.getenv(ENV_KEY_REGISTRATION_TICKET);
        String dbUrl = System.getenv(ENV_KEY_COUCHDB_URL);
        String dbUser = System.getenv(ENV_KEY_COUCHDB_USER);
        String dbPassword = System.getenv(ENV_KEY_COUCHDB_PASSWORD);

        if (ticket == null || ticket.length() < 1) {
            mainLogger.severe(String.format("Missing registration ticket | %s is not set", ENV_KEY_REGISTRATION_TICKET));
            System.exit(1);
        }

        Home home = new Home(ticket);
        DeviceManager deviceManager = new DeviceManager();

        try {
            mainLogger.info(String.format("DB connection info | URL: %s, User: %s", dbUrl, dbUser));
            Registry registry;
            if (dbUser != null && dbUser.length() > 0 && dbPassword != null && dbPassword.length() > 0) {
                registry = new Registry(home.getRegistrationTicket().getDataCollectorId(), dbUrl, dbUser, dbPassword);
            } else {
                registry = new Registry(home.getRegistrationTicket().getDataCollectorId(), dbUrl);
            }

            Gateway gateway = new Gateway(home, deviceManager, registry);
            gateway.run();
        } catch (GatewayException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    // AppIoT Env Keys -------------------------------------------------------------------------------------------------

    private static final String ENV_KEY_REGISTRATION_TICKET = "APPIOT_REGISTRATION_TICKET";

    // -----------------------------------------------------------------------------------------------------------------

    // CouchDB Env Keys ------------------------------------------------------------------------------------------------

    private static final String ENV_KEY_COUCHDB_URL = "APPIOT_COUCHDB_URL";
    private static final String ENV_KEY_COUCHDB_USER = "APPIOT_COUCHDB_USER";
    private static final String ENV_KEY_COUCHDB_PASSWORD = "APPIOT_COUCHDB_PASSWORD";

    // -----------------------------------------------------------------------------------------------------------------

    // Setting Keys ----------------------------------------------------------------------------------------------------

    private static final String OWM_GATEWAY_SETTINGS_NAME = "OWM Gateway Settings";
    private static final String OWM_GATEWAY_API_KEY_NAME = "API-Key";

    // -----------------------------------------------------------------------------------------------------------------

    // OWM Gateway -----------------------------------------------------------------------------------------------------

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private AppIoTGateway appiotGateway;
    private Map<String, CurrentWeatherDevice> pollerRegistry = new HashMap<>();
    private String apiKey;

    public Gateway(Home home, DeviceManager deviceManager, DeviceRegistry registry) {
        super(deviceManager);
        appiotGateway = new AppIoTGateway(this);
        appiotGateway.setHomeDirectory(home);
        appiotGateway.setDeviceRegistry(registry);
    }

    private void run() {
        appiotGateway.start();

        logger.info("Starting AppIoT Gateway");
        SettingCategory settings = appiotGateway.getProperties().getSettingCategory(OWM_GATEWAY_SETTINGS_NAME);
        if (settings != null) {
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

    private void deleteDevice(String endpoint) {
        logger.info(String.format("Deleting poller %s", endpoint));
        CurrentWeatherDevice poller = pollerRegistry.remove(endpoint);
        if (poller != null) {
            poller.stop();
        }
    }

    private void updateDevice(String endpoint) {
        logger.info(String.format("Updating poller %s", endpoint));
        Device device = getDeviceManager().getDevice(endpoint);
        if (device == null) {
            logger.warning(String.format("Can't update device %s, not registered correctly", endpoint));
            return;
        }

        CurrentWeatherDevice poller = pollerRegistry.get(endpoint);
        if (poller == null) {
            createDevice(endpoint);
            return;
        }
        poller.update(device);
    }

    private void createDevice(String endpoint) {
        logger.info(String.format("Starting a new poller %s", endpoint));
        Device device = getDeviceManager().getDevice(endpoint);
        if (device == null) {
            logger.warning(String.format("Device %s not registered correctly", endpoint));
            return;
        }

        if (pollerRegistry.containsKey(endpoint)) {
            logger.warning(String.format("Poller with endpoint %s already exists", endpoint));
            return;
        }

        CurrentWeatherDevice poller = new CurrentWeatherDevice(createOWMClient(apiKey), device);
        pollerRegistry.put(endpoint, poller);
        poller.start();
    }

    // -----------------------------------------------------------------------------------------------------------------

    // AppIoT Listener Override ----------------------------------------------------------------------------------------

    @Override
    public void onDeviceRegisterRequest(String correlationId, String endpoint, DeviceRegisterRequest deviceRegisterRequest) {
        super.onDeviceRegisterRequest(correlationId, endpoint, deviceRegisterRequest);

        switch (deviceRegisterRequest.getOperation()) {
            case Operation.DELETE:
                deleteDevice(endpoint);
                break;
            case Operation.PUT:
                updateDevice(endpoint);
                break;
            case Operation.POST:
                createDevice(endpoint);
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
