package com.eap09.reservas.customerbooking.infrastructure;

import java.time.LocalTime;

public interface AvailableSlotProjection {

    Long getAvailabilityId();

    LocalTime getStartTime();

    LocalTime getEndTime();

    long getRemainingSlots();
}
