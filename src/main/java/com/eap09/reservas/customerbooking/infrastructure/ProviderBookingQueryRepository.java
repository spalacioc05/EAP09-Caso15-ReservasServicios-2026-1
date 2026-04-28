package com.eap09.reservas.customerbooking.infrastructure;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderBookingQueryRepository extends JpaRepository<com.eap09.reservas.customerbooking.domain.ReservationEntity, Long> {

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio
            WHERE s.id_usuario_proveedor = :providerUserId
            """, nativeQuery = true)
    boolean existsAnyReservationForProvider(@Param("providerUserId") Long providerUserId);

    @Query(value = """
            SELECT
                r.id_reserva AS bookingId,
                s.id_servicio AS serviceId,
                s.nombre_servicio AS serviceName,
                ds.id_disponibilidad_servicio AS availabilityId,
                ds.fecha_disponibilidad AS slotDate,
                ds.hora_inicio AS startTime,
                ds.hora_fin AS endTime,
                c.id_usuario AS customerId,
                CONCAT(c.nombres_usuario, ' ', c.apellidos_usuario) AS customerFullName,
                c.correo_usuario AS customerEmail,
                er.nombre_estado AS bookingStatus,
                r.fecha_creacion_reserva AS createdAt
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_servicio s
                ON s.id_servicio = ds.id_servicio
            JOIN tbl_usuario c
                ON c.id_usuario = r.id_usuario_cliente
            JOIN tbl_estado er
                ON er.id_estado = r.id_estado_reserva
            WHERE s.id_usuario_proveedor = :providerUserId
                AND (:serviceId IS NULL OR s.id_servicio = :serviceId)
                AND (:date IS NULL OR ds.fecha_disponibilidad = :date)
                AND (:reservationStateId IS NULL OR r.id_estado_reserva = :reservationStateId)
            ORDER BY ds.fecha_disponibilidad DESC, ds.hora_inicio ASC, r.id_reserva DESC
            """, nativeQuery = true)
    List<ProviderBookingProjection> findProviderBookings(@Param("providerUserId") Long providerUserId,
                                                         @Param("serviceId") Long serviceId,
                                                         @Param("date") LocalDate date,
                                                         @Param("reservationStateId") Long reservationStateId);
}