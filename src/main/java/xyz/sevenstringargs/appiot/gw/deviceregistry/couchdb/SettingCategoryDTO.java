package xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb;

import com.ericsson.appiot.gateway.dto.SettingCategory;

import java.util.ArrayList;
import java.util.List;

public class SettingCategoryDTO {
    private String name;
    private List<SettingsDTO> settings = new ArrayList<>();

    public SettingCategoryDTO(){

    }

    public SettingCategoryDTO(SettingCategory setcat){
        name = setcat.getName();
        setcat.getSettings().stream().forEach(s -> settings.add(new SettingsDTO((s))));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SettingsDTO> getSettings() {
        return settings;
    }

    public void setSettings(List<SettingsDTO> settings) {
        this.settings = settings;
    }

    public SettingCategory toAppIoTModel(){
        SettingCategory cat = new SettingCategory(name);
        settings.stream().forEach(s -> cat.addSetting(s.toAppIoTModel()));
        return cat;
    }
}
