package com.eap09.reservas.customerbooking.infrastructure;

public interface AvailableOfferProjection {

    Long getServiceId();

    String getServiceName();

    String getServiceDescription();

    String getProviderName();
}
