package com.eap09.reservas.customerbooking.infrastructure;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

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

    OffsetDateTime getCreatedAt();
}