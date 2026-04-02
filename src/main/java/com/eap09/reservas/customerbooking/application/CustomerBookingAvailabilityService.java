package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingAvailabilityRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerBookingAvailabilityService {

    private final CustomerBookingAvailabilityRepository repository;

    public CustomerBookingAvailabilityService(CustomerBookingAvailabilityRepository repository) {
        this.repository = repository;
    }

    public List<AvailabilityResponse> getAvailability(Long idServicio, LocalDate fecha) {
        return repository.findAvailability(idServicio, fecha);
    }
}