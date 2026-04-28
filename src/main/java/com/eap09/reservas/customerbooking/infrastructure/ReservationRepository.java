package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    long countByIdDisponibilidadServicio(Long idDisponibilidadServicio);

    long countByIdDisponibilidadServicioAndIdEstadoReserva(Long idDisponibilidadServicio, Long idEstadoReserva);

    @Query(value = """
            SELECT
                r.id_reserva AS bookingId,
                s.id_servicio AS serviceId,
                s.id_usuario_proveedor AS providerUserId,
                r.id_usuario_cliente AS customerUserId,
                r.id_estado_reserva AS reservationStateId,
                e.nombre_estado AS reservationStateName,
                ds.fecha_disponibilidad AS slotDate,
                ds.hora_inicio AS slotStartTime,
                ds.hora_fin AS slotEndTime
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_servicio s
                ON s.id_servicio = ds.id_servicio
            JOIN tbl_estado e
                ON e.id_estado = r.id_estado_reserva
            WHERE r.id_reserva = :bookingId
            """, nativeQuery = true)
    Optional<BookingLifecycleProjection> findBookingLifecycleById(@Param("bookingId") Long bookingId);

    @Query(value = """
            SELECT
                r.id_reserva AS bookingId,
                s.id_servicio AS serviceId,
                s.nombre_servicio AS serviceName,
                p.id_usuario AS providerId,
                CONCAT(p.nombres_usuario, ' ', p.apellidos_usuario) AS providerFullName,
                ds.fecha_disponibilidad AS slotDate,
                ds.hora_inicio AS startTime,
                ds.hora_fin AS endTime,
                e.nombre_estado AS bookingStatus,
                r.fecha_creacion_reserva AS createdAt
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_servicio s
                ON s.id_servicio = ds.id_servicio
            JOIN tbl_usuario p
                ON p.id_usuario = s.id_usuario_proveedor
            JOIN tbl_estado e
                ON e.id_estado = r.id_estado_reserva
            WHERE r.id_usuario_cliente = :customerUserId
            ORDER BY ds.fecha_disponibilidad DESC, ds.hora_inicio ASC, r.id_reserva DESC
            """, nativeQuery = true)
    List<CustomerReservationProjection> findByCustomerUserId(@Param("customerUserId") Long customerUserId);

}