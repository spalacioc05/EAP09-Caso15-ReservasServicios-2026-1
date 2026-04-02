package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingServiceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerBookingOfferService {

    private final CustomerBookingServiceRepository repository;

    public CustomerBookingOfferService(CustomerBookingServiceRepository repository) {
        this.repository = repository;
    }

    public List<OfferResponse> getAvailableServices() {

        List<OfferResponse> offers = repository.findAvailableOffers();

        if (offers.isEmpty()) {
            throw new RuntimeException("No hay oferta disponible");
        }

        return offers;
    }
}