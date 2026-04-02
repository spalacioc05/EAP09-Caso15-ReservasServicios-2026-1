package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicesRepository extends JpaRepository<ServiceEntity, Long> {
}