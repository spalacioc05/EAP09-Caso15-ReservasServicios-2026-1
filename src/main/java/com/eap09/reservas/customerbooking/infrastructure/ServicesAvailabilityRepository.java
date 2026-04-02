package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicesAvailabilityRepository extends JpaRepository<ServiceAvailabilityEntity, Long> {
}