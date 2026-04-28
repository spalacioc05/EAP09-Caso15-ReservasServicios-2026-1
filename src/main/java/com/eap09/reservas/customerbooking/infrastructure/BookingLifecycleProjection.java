package com.eap09.reservas.customerbooking.infrastructure;

import java.time.LocalDate;
import java.time.LocalTime;

public interface BookingLifecycleProjection {

    Long getBookingId();

    Long getServiceId();

    Long getProviderUserId();

    Long getCustomerUserId();

    Long getReservationStateId();

    String getReservationStateName();

    LocalDate getSlotDate();

    LocalTime getSlotStartTime();

    LocalTime getSlotEndTime();
}