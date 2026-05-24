package com.eap09.reservas.administration.infrastructure;

public interface BookingsByProviderProjection {

    Long getProviderId();

    String getProviderName();

    long getTotal();
}