package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerBookingAvailabilityRepository extends JpaRepository<ServiceAvailabilityEntity, Long> {

    @Query("""
        SELECT new com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse(
            d.idDisponibilidadServicio,
            d.horaInicio,
            d.horaFin,
            (s.capacidadMaximaConcurrente - COUNT(r.idReserva))
        )
        FROM ServiceAvailabilityEntity d
        JOIN com.eap09.reservas.provideroffer.domain.ServiceEntity s
            ON d.idServicio = s.idServicio
        LEFT JOIN com.eap09.reservas.customerbooking.domain.ReservationEntity r
            ON r.idDisponibilidadServicio = d.idDisponibilidadServicio
            AND r.idEstadoReserva = 7
        WHERE d.idServicio = :idServicio
          AND d.fechaDisponibilidad = :fecha
          AND d.idEstadoDisponibilidad = 5
        GROUP BY d.idDisponibilidadServicio, d.horaInicio, d.horaFin, s.capacidadMaximaConcurrente
        HAVING (s.capacidadMaximaConcurrente - COUNT(r.idReserva)) > 0
    """)
    List<AvailabilityResponse> findAvailability(Long idServicio, LocalDate fecha);
}