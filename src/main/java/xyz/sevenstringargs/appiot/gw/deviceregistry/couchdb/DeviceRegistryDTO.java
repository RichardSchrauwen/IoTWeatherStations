package xyz.sevenstringargs.appiot.gw.deviceregistry.couchdb;

import com.ericsson.appiot.gateway.deviceregistry.DeviceRegistration;
import com.ericsson.appiot.gateway.dto.DeviceRegisterRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ektorp.support.CouchDbDocument;

public class DeviceRegistryDTO extends CouchDbDocument{

    private String id;
    private String gatewayId;
    private DeviceRegisterRequest deviceRegistration;

    public DeviceRegistryDTO(){

    }

    public DeviceRegistryDTO(String gatewayId, DeviceRegisterRequest deviceRegistration) {
        this.id = deviceRegistration.getEndpoint();
        this.gatewayId = gatewayId;
        this.deviceRegistration = deviceRegistration;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public DeviceRegisterRequest getDeviceRegistration() {
        return deviceRegistration;
    }

    public void setDeviceRegistration(DeviceRegisterRequest deviceRegistration) {
        this.deviceRegistration = deviceRegistration;
    }

    @JsonProperty("_id")
    @Override
    public String getId(){
        return id;
    }

    @JsonProperty("_id")
    @Override
    public void setId(String id){
        this.id = id;
    }
}
