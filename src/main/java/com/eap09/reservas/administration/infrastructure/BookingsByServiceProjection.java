package com.eap09.reservas.administration.infrastructure;

public interface BookingsByServiceProjection {

    Long getServiceId();

    String getServiceName();

    long getTotal();
}