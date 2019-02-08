package com.ericcson.appiot.examples.gw.deviceregistry.couchdb.dto;

import com.ericsson.appiot.gateway.deviceregistry.DeviceRegistration;
import com.ericsson.appiot.gateway.dto.ResourceLink;
import com.ericsson.appiot.gateway.dto.SettingCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ektorp.support.CouchDbDocument;

import java.util.ArrayList;
import java.util.List;

public class DeviceRegistrationDTO extends CouchDbDocument {

    // ID property for CouchDBDocument
    @JsonProperty("_id")
    private String endpoint;
    private String gatewayId;
    private String name;
    private boolean internal;
    private List<ResourceLink> resourceLinks;
    private List<SettingCategoryDTO> settingCategories = new ArrayList<>();

    // Override CouchDBDocument ID -------------------------------------------------------------------------------------

    @Override
    public void setId(String id) {
        endpoint = id;
    }

    @Override
    public String getId() {
        return endpoint;
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Getters & Setters -----------------------------------------------------------------------------------------------

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ResourceLink> getResourceLinks() {
        return resourceLinks;
    }

    public void setResourceLinks(List<ResourceLink> resourceLinks) {
        this.resourceLinks = resourceLinks;
    }

    public List<SettingCategoryDTO> getSettingCategories() {
        return settingCategories;
    }

    public void setSettingCategories(List<SettingCategoryDTO> settingCategories) {
        this.settingCategories = settingCategories;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    // -----------------------------------------------------------------------------------------------------------------

    // Helper methods ----------------------------------------------------------------------------------------------

    public static DeviceRegistration toAppIoTModel(DeviceRegistrationDTO dto) {
        DeviceRegistration reg = new DeviceRegistration();
        reg.setResourceLinks(dto.getResourceLinks());
        reg.setName(dto.getName());
        reg.setInternal(dto.isInternal());
        reg.setEndpoint(dto.getEndpoint());
        List<SettingCategory> categories = new ArrayList<>();
        dto.getSettingCategories().stream().forEach(s -> categories.add(s.toAppIoTModel()));
        reg.setSettingCategories(categories);
        return reg;
    }

    public static DeviceRegistrationDTO fromAppIoTModel(String gatewayId, DeviceRegistration reg) {
        DeviceRegistrationDTO dto = new DeviceRegistrationDTO();
        dto.setEndpoint(reg.getEndpoint());
        dto.setName(reg.getName());
        dto.setGatewayId(gatewayId);
        dto.setResourceLinks(reg.getResourceLinks());
        List<SettingCategoryDTO> categoryDTOS = new ArrayList<>();
        reg.getSettingCategories().stream().forEach(s -> categoryDTOS.add(new SettingCategoryDTO(s)));
        dto.setSettingCategories(categoryDTOS);
        return dto;
    }

    // -------------------------------------------------------------------------------------------------------------
}
