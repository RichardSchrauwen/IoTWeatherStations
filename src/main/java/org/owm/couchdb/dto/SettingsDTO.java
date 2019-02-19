package org.owm.couchdb.dto;

import com.ericsson.appiot.gateway.dto.Setting;

public class SettingsDTO {
    private String key;
    private String value;
    private String dataType;

    // Constructors ----------------------------------------------------------------------------------------------------

    public SettingsDTO() {}

    public SettingsDTO(Setting setting) {
        key = setting.getKey();
        value = setting.getValue();
        dataType = setting.getDataType();
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Getters & Setters -----------------------------------------------------------------------------------------------

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Helper Methods --------------------------------------------------------------------------------------------------

    public Setting toAppIoTModel() {
        Setting setting = new Setting();
        setting.setDataType(dataType);
        setting.setKey(key);
        setting.setValue(value);
        return setting;
    }

    // -----------------------------------------------------------------------------------------------------------------
}
