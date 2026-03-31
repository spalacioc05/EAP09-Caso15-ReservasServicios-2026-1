package com.eap09.reservas.provideroffer.infrastructure;

import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceAvailabilityRepository extends JpaRepository<ServiceAvailabilityEntity, Long> {

    Optional<ServiceAvailabilityEntity> findByIdDisponibilidadServicioAndIdServicio(Long idDisponibilidadServicio,
                                                                                    Long idServicio);

    @Query("""
            SELECT COUNT(a) > 0
            FROM ServiceAvailabilityEntity a
            WHERE a.idServicio = :serviceId
              AND a.fechaDisponibilidad = :availabilityDate
              AND :startTime < a.horaFin
              AND :endTime > a.horaInicio
            """)
    boolean existsOverlappingRange(@Param("serviceId") Long serviceId,
                                   @Param("availabilityDate") LocalDate availabilityDate,
                                   @Param("startTime") LocalTime startTime,
                                   @Param("endTime") LocalTime endTime);
}