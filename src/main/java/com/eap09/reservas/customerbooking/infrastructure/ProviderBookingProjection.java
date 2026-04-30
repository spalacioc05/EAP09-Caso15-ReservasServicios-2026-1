package com.eap09.reservas.customerbooking.infrastructure;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;

public interface ProviderBookingProjection {

    Long getBookingId();

    Long getServiceId();

    String getServiceName();

    Long getAvailabilityId();

    LocalDate getSlotDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    Long getCustomerId();

    String getCustomerFullName();

    String getCustomerEmail();

    String getBookingStatus();

    Instant getCreatedAt();
}