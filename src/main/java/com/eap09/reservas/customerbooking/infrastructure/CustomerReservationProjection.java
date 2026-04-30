package com.eap09.reservas.customerbooking.infrastructure;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;

public interface CustomerReservationProjection {

    Long getBookingId();

    Long getServiceId();

    String getServiceName();

    Long getProviderId();

    String getProviderFullName();

    LocalDate getSlotDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    String getBookingStatus();

    Instant getCreatedAt();
}