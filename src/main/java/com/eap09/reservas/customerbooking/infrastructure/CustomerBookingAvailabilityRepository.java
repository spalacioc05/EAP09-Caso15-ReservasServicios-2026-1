package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerBookingAvailabilityRepository extends JpaRepository<ServiceAvailabilityEntity, Long> {

        @Query(value = """
                        SELECT COUNT(*) > 0
                        FROM tbl_servicio s
                        JOIN tbl_usuario u ON u.id_usuario = s.id_usuario_proveedor
                        WHERE s.id_servicio = :serviceId
                            AND s.id_usuario_proveedor = :providerId
                            AND s.id_estado_servicio = (
                                        SELECT e.id_estado
                                        FROM tbl_estado e
                                        JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                                        WHERE c.nombre_categoria_estado = 'tbl_servicio'
                                            AND e.nombre_estado = 'ACTIVO'
                            )
                            AND u.id_estado_usuario = (
                                        SELECT e.id_estado
                                        FROM tbl_estado e
                                        JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                                        WHERE c.nombre_categoria_estado = 'tbl_usuario'
                                            AND e.nombre_estado = 'ACTIVA'
                            )
                        """, nativeQuery = true)
        boolean existsValidProviderServiceRelation(@Param("providerId") Long providerId,
                                                                                             @Param("serviceId") Long serviceId);

        @Query(value = """
                        SELECT
                                ds.id_disponibilidad_servicio AS availabilityId,
                                ds.hora_inicio AS startTime,
                                ds.hora_fin AS endTime,
                                (s.capacidad_maxima_concurrente - COUNT(r.id_reserva)) AS remainingSlots
                        FROM tbl_disponibilidad_servicio ds
                        JOIN tbl_servicio s
                                ON s.id_servicio = ds.id_servicio
                        JOIN tbl_usuario u
                                ON u.id_usuario = s.id_usuario_proveedor
                        LEFT JOIN tbl_reserva r
                                ON r.id_disponibilidad_servicio = ds.id_disponibilidad_servicio
                             AND r.id_estado_reserva = (
                                        SELECT e.id_estado
                                        FROM tbl_estado e
                                        JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                                        WHERE c.nombre_categoria_estado = 'tbl_reserva'
                                            AND e.nombre_estado = 'CREADA'
                             )
                        WHERE s.id_usuario_proveedor = :providerId
                            AND s.id_servicio = :serviceId
                            AND ds.fecha_disponibilidad = :date
                            AND s.id_estado_servicio = (
                                        SELECT e.id_estado
                                        FROM tbl_estado e
                                        JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                                        WHERE c.nombre_categoria_estado = 'tbl_servicio'
                                            AND e.nombre_estado = 'ACTIVO'
                            )
                            AND u.id_estado_usuario = (
                                        SELECT e.id_estado
                                        FROM tbl_estado e
                                        JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                                        WHERE c.nombre_categoria_estado = 'tbl_usuario'
                                            AND e.nombre_estado = 'ACTIVA'
                            )
                            AND ds.id_estado_disponibilidad = (
                                        SELECT e.id_estado
                                        FROM tbl_estado e
                                        JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                                        WHERE c.nombre_categoria_estado = 'tbl_disponibilidad_servicio'
                                            AND e.nombre_estado = 'HABILITADA'
                            )
                        GROUP BY ds.id_disponibilidad_servicio, ds.hora_inicio, ds.hora_fin, s.capacidad_maxima_concurrente
                        HAVING (s.capacidad_maxima_concurrente - COUNT(r.id_reserva)) > 0
                        ORDER BY ds.hora_inicio
                        """, nativeQuery = true)
        List<AvailableSlotProjection> findReservableAvailabilities(@Param("providerId") Long providerId,
                                                                                                                             @Param("serviceId") Long serviceId,
                                                                                                                             @Param("date") LocalDate date);
}