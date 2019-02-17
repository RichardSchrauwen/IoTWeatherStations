package com.ericcson.appiot.examples.gw.owm;

import com.ericsson.appiot.gateway.GatewayException;
import com.ericsson.appiot.gateway.core.DefaultHomeDirectory;
import com.ericsson.appiot.gateway.dto.RegistrationTicket;
import com.google.gson.Gson;

public class Home extends DefaultHomeDirectory {
    private RegistrationTicket registrationTicket;

    public Home(String registrationTicketJSON) {
        registrationTicket = new Gson().fromJson(registrationTicketJSON, RegistrationTicket.class);
    }

    // In-Memory Registration Ticket -----------------------------------------------------------------------------------

    @Override
    public void saveRegistrationTicket(RegistrationTicket registrationTicket) throws GatewayException {
        this.registrationTicket = registrationTicket;
    }

    @Override
    public RegistrationTicket getRegistrationTicket() throws GatewayException {
        return this.registrationTicket;
    }

    @Override
    public void deleteRegistrationTicket() throws GatewayException {
        this.registrationTicket = null;
    }

    // -----------------------------------------------------------------------------------------------------------------
}
