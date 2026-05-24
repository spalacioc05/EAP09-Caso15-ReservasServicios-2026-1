package com.eap09.reservas.administration.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public interface AdminReservationSupervisionProjection {

    Long getBookingId();

    Long getCustomerId();

    String getCustomerFullName();

    String getCustomerEmail();

    Long getProviderId();

    String getProviderFullName();

    String getProviderEmail();

    Long getServiceId();

    String getServiceName();

    Long getAvailabilityId();

    LocalDate getSlotDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    String getBookingStatus();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    Instant getCancelledAt();

    Instant getFinishedAt();
}