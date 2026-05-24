package com.eap09.reservas.administration.infrastructure;

import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface OperationalReportRepository extends Repository<ReservationEntity, Long> {

    @Query(value = """
            SELECT COUNT(*)
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            WHERE (:fromDate IS NULL OR ds.fecha_disponibilidad >= :fromDate)
              AND (:toDate IS NULL OR ds.fecha_disponibilidad <= :toDate)
            """, nativeQuery = true)
    long countBookings(@Param("fromDate") LocalDate fromDate,
                       @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
                e.nombre_estado AS status,
                COUNT(*) AS total
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_estado e
                ON e.id_estado = r.id_estado_reserva
            WHERE (:fromDate IS NULL OR ds.fecha_disponibilidad >= :fromDate)
              AND (:toDate IS NULL OR ds.fecha_disponibilidad <= :toDate)
            GROUP BY e.nombre_estado
            ORDER BY CASE e.nombre_estado
                WHEN 'CREADA' THEN 1
                WHEN 'FINALIZADA' THEN 2
                WHEN 'CANCELADA' THEN 3
                ELSE 99
            END, e.nombre_estado
            """, nativeQuery = true)
    List<BookingsByStatusProjection> countBookingsByStatus(@Param("fromDate") LocalDate fromDate,
                                                           @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
                s.id_servicio AS serviceId,
                s.nombre_servicio AS serviceName,
                COUNT(*) AS total
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_servicio s
                ON s.id_servicio = ds.id_servicio
            WHERE (:fromDate IS NULL OR ds.fecha_disponibilidad >= :fromDate)
              AND (:toDate IS NULL OR ds.fecha_disponibilidad <= :toDate)
            GROUP BY s.id_servicio, s.nombre_servicio
            ORDER BY COUNT(*) DESC, s.nombre_servicio ASC, s.id_servicio ASC
            """, nativeQuery = true)
    List<BookingsByServiceProjection> countBookingsByService(@Param("fromDate") LocalDate fromDate,
                                                             @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
                p.id_usuario AS providerId,
                CONCAT(p.nombres_usuario, ' ', p.apellidos_usuario) AS providerName,
                COUNT(*) AS total
            FROM tbl_reserva r
            JOIN tbl_disponibilidad_servicio ds
                ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
            JOIN tbl_servicio s
                ON s.id_servicio = ds.id_servicio
            JOIN tbl_usuario p
                ON p.id_usuario = s.id_usuario_proveedor
            WHERE (:fromDate IS NULL OR ds.fecha_disponibilidad >= :fromDate)
              AND (:toDate IS NULL OR ds.fecha_disponibilidad <= :toDate)
            GROUP BY p.id_usuario, p.nombres_usuario, p.apellidos_usuario
            ORDER BY COUNT(*) DESC, providerName ASC, p.id_usuario ASC
            """, nativeQuery = true)
    List<BookingsByProviderProjection> countBookingsByProvider(@Param("fromDate") LocalDate fromDate,
                                                               @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT COALESCE(SUM(capacity_by_availability.capacity), 0)
            FROM (
                SELECT
                    ds.id_disponibilidad_servicio,
                    MAX(s.capacidad_maxima_concurrente) AS capacity
                FROM tbl_reserva r
                JOIN tbl_disponibilidad_servicio ds
                    ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
                JOIN tbl_servicio s
                    ON s.id_servicio = ds.id_servicio
                WHERE (:fromDate IS NULL OR ds.fecha_disponibilidad >= :fromDate)
                  AND (:toDate IS NULL OR ds.fecha_disponibilidad <= :toDate)
                GROUP BY ds.id_disponibilidad_servicio
            ) capacity_by_availability
            """, nativeQuery = true)
    long calculateTotalOfferedCapacity(@Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate);
}