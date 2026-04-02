package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    long countByIdDisponibilidadServicio(Long idDisponibilidadServicio);

}