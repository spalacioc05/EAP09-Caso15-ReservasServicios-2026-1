package com.eap09.reservas.administration.infrastructure;

public interface BookingsByStatusProjection {

    String getStatus();

    long getTotal();
}