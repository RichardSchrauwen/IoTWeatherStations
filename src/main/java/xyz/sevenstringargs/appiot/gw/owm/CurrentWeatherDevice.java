package xyz.sevenstringargs.appiot.gw.owm;

import com.ericsson.appiot.gateway.GatewayException;
import com.ericsson.appiot.gateway.device.Device;
import com.ericsson.appiot.gateway.device.smartobject.SmartObject;
import com.ericsson.appiot.gateway.device.smartobject.resource.Resource;
import com.ericsson.appiot.gateway.device.smartobject.resource.type.ResourceBase;
import com.ericsson.appiot.gateway.dto.SettingCategory;
import com.ericsson.appiot.gateway.senml.SenMlException;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import java.util.Optional;
import java.util.logging.Logger;

public class CurrentWeatherDevice implements Runnable {

    // IPSO constants --------------------------------------------------------------------------------------------------

    private static final int RESOURCE_MAX_VALUE_ID = 5601;
    private static final int RESOURCE_MIN_VALUE_ID = 5602;
    private static final int RESOURCE_VALUE_ID = 5700;
    private static final int RESOURCE_UNIT_ID = 5701;
    private static final int RESOURCE_LAT_ID = 5514;
    private static final int RESOURCE_LON_ID = 5515;

    private static final int SMART_OBJECT_TEMP_ID = 3303;
    private static final int SMART_OBJECT_HUM_ID = 3304;
    private static final int SMART_OBJECT_BAR_ID = 3315;
    private static final int SMART_OBJECT_LOC_ID = 3336;

    // -----------------------------------------------------------------------------------------------------------------

    // AppIoT Device Settings ------------------------------------------------------------------------------------------

    private static final String OWM_SETTING_CATEGORY_NAME = "OWM Device Settings";
    private static final String OWM_SETTING_INTERVAL_NAME = "Interval";
    private static final String OWM_SETTING_CITY_ID_NAME = "CityID";
    private static final String OWM_SETTING_CITY_ENABLED_NAME = "Enabled";

    // -----------------------------------------------------------------------------------------------------------------

    // Default settings ------------------------------------------------------------------------------------------------

    private static final boolean ENABLED_UNSET = false;
    private static final int INTERVAL_MIN = 1;
    private static final int INTERVAL_UNSET = -1;
    private static final long CITY_ID_UNSET = -1;

    // -----------------------------------------------------------------------------------------------------------------

    // Settings --------------------------------------------------------------------------------------------------------

    private int interval;
    private boolean enabled;
    private long cityId;

    // -----------------------------------------------------------------------------------------------------------------

    // Temperature Object ----------------------------------------------------------------------------------------------

    private Resource temperature5601Resource;
    private Resource temperature5602Resource;
    private Resource temperature5700Resource;
    private Resource temperature5701Resource;

    private float temperatureMin;
    private float temperatureMax;
    private float temperature;
    private String temperatureUnit;

    // -----------------------------------------------------------------------------------------------------------------

    // Humidity Object -------------------------------------------------------------------------------------------------

    private Resource humidity5700Resource;
    private Resource humidity5701Resource;

    private float humidity;
    private String humidityUnit;

    // -----------------------------------------------------------------------------------------------------------------

    // Barometer/Pressure Object ---------------------------------------------------------------------------------------

    private Resource barometer5700Resource;
    private Resource barometer5701Resource;

    private float pressure;
    private String pressureUnit;

    // -----------------------------------------------------------------------------------------------------------------

    // Location Object -------------------------------------------------------------------------------------------------

    private Resource location5514Resource;
    private Resource location5515Resource;

    private float lat;
    private float lon;

    // -----------------------------------------------------------------------------------------------------------------

    // Other -----------------------------------------------------------------------------------------------------------

    private static final int MILLI_SECONDS = 1000;

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    // -----------------------------------------------------------------------------------------------------------------

    // Poller ----------------------------------------------------------------------------------------------------------

    private Device device;
    private OpenWeatherMap owmClient;
    private boolean running;

    // -----------------------------------------------------------------------------------------------------------------

    public CurrentWeatherDevice(OpenWeatherMap owmClient, Device device) {
        this.running = false;
        this.device = device;
        this.owmClient = owmClient;

        this.temperatureMin = Float.MIN_VALUE;
        this.temperatureMax = Float.MIN_VALUE;
        this.temperature = Float.MIN_VALUE;
        this.temperatureUnit = "";

        this.humidity = Float.MIN_VALUE;
        this.humidityUnit = "";

        this.pressure = Float.MIN_VALUE;
        this.pressureUnit = "";

        this.lat = Float.MIN_VALUE;
        this.lon = Float.MIN_VALUE;

        clearSettings();
        updateSettings();
    }

    // Internal Execution Loop / Poll ---------------------------------------------------------------------------------

    private void loop() {
        for (SmartObject so : device.getSmartObjects()) {
            for (Resource r : so.getResources()) {
                ((ResourceBase) r).requestObserve();
            }
        }

        logger.info(String.format("OWMPoller %s starting", device.getEndpoint()));
        while (running) {
            if (enabled()) {
                try {
                    poll();
                } catch (SenMlException e) {
                    e.printStackTrace();
                } catch (GatewayException e) {
                    e.printStackTrace();
                }

                try {
                    logger.info(String.format("OWMPoller %s sleeping for %d seconds", device.getEndpoint(), interval / MILLI_SECONDS));
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    logger.info(String.format("OWMPoller %s not configured correctly", device.getEndpoint()));
                    logger.info(String.format("OWMPoller %s sleeping for %d seconds", device.getEndpoint(), 60));
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        logger.info(String.format("OWMPoller %s shutting down", device.getEndpoint()));
    }

    private void poll() throws SenMlException, GatewayException {
        logger.info(String.format("Polling for city %d with device %s", cityId, device.getEndpoint()));


        CurrentWeather data = owmClient.currentWeatherByCityCode(cityId);
        if (data == null) {
            logger.info(String.format("Failed to get data for city %d with device %s", cityId, device.getEndpoint()));
            return;
        }

        if (!data.hasMainInstance()) {
            return;
        }
        CurrentWeather.Main main = data.getMainInstance();

        // Temperature readings ----------------------------------------------------------------------------------------

        if (main.hasTemperature() && main.getTemperature() != temperature) {
            temperature = main.getTemperature();
            device.onResourceValueChanged(temperature5700Resource, temperature);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), temperature5700Resource.getPath(), temperature));
        }

        if (main.hasMinTemperature() && main.getMinTemperature() != temperatureMin) {
            temperatureMin = main.getMinTemperature();
            device.onResourceValueChanged(temperature5601Resource, temperatureMin);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), temperature5601Resource.getPath(), temperatureMin));
        }

        if (main.hasMaxTemperature() && main.getMaxTemperature() != temperatureMax) {
            temperatureMax = main.getMaxTemperature();
            device.onResourceValueChanged(temperature5602Resource, temperatureMax);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), temperature5602Resource.getPath(), temperatureMax));
        }

        if (!temperatureUnit.equals("c")) {
            temperatureUnit = "c";
            device.onResourceValueChanged(temperature5701Resource, temperatureUnit);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), temperature5701Resource.getPath(), temperatureUnit));
        }

        // -------------------------------------------------------------------------------------------------------------

        // Humidity readings -------------------------------------------------------------------------------------------

        if (main.hasHumidity() && main.getHumidity() != humidity) {
            humidity = main.getHumidity();
            device.onResourceValueChanged(humidity5700Resource, humidity);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), humidity5700Resource.getPath(), humidity));
        }

        if (!humidityUnit.equals("%")) {
            humidityUnit = "%";
            device.onResourceValueChanged(humidity5701Resource, humidityUnit);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), humidity5701Resource.getPath(), humidityUnit));
        }

        // -------------------------------------------------------------------------------------------------------------

        // Pressure readings -------------------------------------------------------------------------------------------

        if (main.hasPressure() && main.getPressure() != pressure) {
            pressure = main.getPressure();
            device.onResourceValueChanged(barometer5700Resource, pressure);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), barometer5700Resource.getPath(), pressure));
        }

        if (!pressureUnit.equals("hpa")) {
            pressureUnit = "hpa";
            device.onResourceValueChanged(barometer5701Resource, pressureUnit);
            logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), barometer5701Resource.getPath(), pressureUnit));
        }

        // -------------------------------------------------------------------------------------------------------------

        // Location readings -------------------------------------------------------------------------------------------

        if (data.hasCoordInstance()) {

            CurrentWeather.Coord coord = data.getCoordInstance();
            if (coord.hasLatitude() && coord.getLatitude() != lat) {
                lat = coord.getLatitude();
                device.onResourceValueChanged(location5514Resource, Float.toString(lat));
                logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), location5514Resource.getPath(), Float.toString(lat)));
            }

            if (coord.hasLongitude() && coord.getLongitude() != lon) {
                lon = coord.getLongitude();
                device.onResourceValueChanged(location5515Resource, Float.toString(lon));
                logger.info(String.format("Sent value from device %s: %s -> %s", device.getEndpoint(), location5515Resource.getPath(), Float.toString(lon)));
            }
        }

        // -------------------------------------------------------------------------------------------------------------
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Poller Settings -------------------------------------------------------------------------------------------------

    private boolean validCityId() {
        return cityId != CITY_ID_UNSET;
    }

    private boolean validInterval() {
        return interval >= INTERVAL_MIN;
    }

    private boolean enabled() {
        return validInterval() && validCityId() && enabled;
    }

    private void clearSettings() {
        this.enabled = ENABLED_UNSET;
        this.interval = INTERVAL_UNSET;
        this.cityId = CITY_ID_UNSET;
        this.temperature5700Resource = null;
        this.humidity5700Resource = null;
        logger.info(String.format("Cleared settings for device %s", device.getEndpoint()));
    }

    private void updateSettings() {
        Optional<SettingCategory> settingsOptional = device.
                getDeviceRegistration().
                getSettingCategories().
                stream().
                filter(s -> s.getName().equals(OWM_SETTING_CATEGORY_NAME)).
                findFirst();

        if (!settingsOptional.isPresent()) {
            return;
        }
        SettingCategory settings = settingsOptional.get();

        interval = Integer.parseInt(settings.getSettingValue(OWM_SETTING_INTERVAL_NAME)) * MILLI_SECONDS;
        cityId = Long.parseLong(settings.getSettingValue(OWM_SETTING_CITY_ID_NAME));
        enabled = Boolean.parseBoolean(settings.getSettingValue(OWM_SETTING_CITY_ENABLED_NAME));

        SmartObject tempSO = device.getSmartObjectInstance(SMART_OBJECT_TEMP_ID, 0);
        if (tempSO != null) {
            temperature5601Resource = tempSO.getResource(RESOURCE_MIN_VALUE_ID);
            temperature5602Resource = tempSO.getResource(RESOURCE_MAX_VALUE_ID);
            temperature5700Resource = tempSO.getResource(RESOURCE_VALUE_ID);
            temperature5701Resource = tempSO.getResource(RESOURCE_UNIT_ID);
        }

        SmartObject humSO = device.getSmartObjectInstance(SMART_OBJECT_HUM_ID, 0);
        if (humSO != null) {
            humidity5700Resource = humSO.getResource(RESOURCE_VALUE_ID);
            humidity5701Resource = humSO.getResource(RESOURCE_UNIT_ID);
        }

        SmartObject barSO = device.getSmartObjectInstance(SMART_OBJECT_BAR_ID, 0);
        if (barSO != null) {
            barometer5700Resource = barSO.getResource(RESOURCE_VALUE_ID);
            barometer5701Resource = barSO.getResource(RESOURCE_UNIT_ID);
        }

        SmartObject locSO = device.getSmartObjectInstance(SMART_OBJECT_LOC_ID, 0);
        if (locSO != null) {
            location5514Resource = locSO.getResource(RESOURCE_LAT_ID);
            location5515Resource = locSO.getResource(RESOURCE_LON_ID);
        }

        logger.info(String.format("Updated settings for device %s", device.getEndpoint()));
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Poller Control Methods -----------------------------------------------------------------------------------------

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;

        new Thread(this).start();
    }

    public synchronized void update(Device device) {
        this.device = device;
        clearSettings();
        updateSettings();
    }

    public synchronized void update(OpenWeatherMap owmClient) {
        this.owmClient = owmClient;
    }

    public synchronized void stop() {
        running = false;
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Runnable interface ----------------------------------------------------------------------------------------------

    @Override
    public void run() {
        loop();
    }

    // -----------------------------------------------------------------------------------------------------------------
}
