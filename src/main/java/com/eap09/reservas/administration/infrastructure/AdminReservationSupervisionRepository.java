package com.eap09.reservas.administration.infrastructure;

import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AdminReservationSupervisionRepository extends Repository<ReservationEntity, Long> {

    @Query(value = """
            SELECT
                r.id_reserva AS bookingId,
                c.id_usuario AS customerId,
                CONCAT(c.nombres_usuario, ' ', c.apellidos_usuario) AS customerFullName,
                c.correo_usuario::text AS customerEmail,
                p.id_usuario AS providerId,
                CONCAT(p.nombres_usuario, ' ', p.apellidos_usuario) AS providerFullName,
                p.correo_usuario::text AS providerEmail,
                s.id_servicio AS serviceId,
                s.nombre_servicio AS serviceName,
                ds.id_disponibilidad_servicio AS availabilityId,
                ds.fecha_disponibilidad AS slotDate,
                ds.hora_inicio AS startTime,
                ds.hora_fin AS endTime,
                er.nombre_estado AS bookingStatus,
                r.fecha_creacion_reserva AS createdAt,
                r.fecha_actualizacion_reserva AS updatedAt,
                r.fecha_cancelacion_reserva AS cancelledAt,
                r.fecha_finalizacion_reserva AS finishedAt
            FROM tbl_reserva r
            JOIN tbl_usuario c
                ON c.id_usuario = r.id_usuario_cliente
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_servicio s
                ON s.id_servicio = ds.id_servicio
            JOIN tbl_usuario p
                ON p.id_usuario = s.id_usuario_proveedor
            JOIN tbl_estado er
                ON er.id_estado = r.id_estado_reserva
            WHERE (:customerId IS NULL OR r.id_usuario_cliente = :customerId)
              AND (:providerId IS NULL OR s.id_usuario_proveedor = :providerId)
              AND (:serviceId IS NULL OR s.id_servicio = :serviceId)
              AND (:reservationStateId IS NULL OR r.id_estado_reserva = :reservationStateId)
              AND (:fromDate IS NULL OR ds.fecha_disponibilidad >= :fromDate)
              AND (:toDate IS NULL OR ds.fecha_disponibilidad <= :toDate)
            ORDER BY ds.fecha_disponibilidad DESC, ds.hora_inicio ASC, r.id_reserva DESC
            """, nativeQuery = true)
    List<AdminReservationSupervisionProjection> findReservations(@Param("customerId") Long customerId,
                                                                 @Param("providerId") Long providerId,
                                                                 @Param("serviceId") Long serviceId,
                                                                 @Param("reservationStateId") Long reservationStateId,
                                                                 @Param("fromDate") LocalDate fromDate,
                                                                 @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM tbl_reserva
            """, nativeQuery = true)
    boolean existsAnyReservation();
}