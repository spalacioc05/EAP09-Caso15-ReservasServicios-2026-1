package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerBookingServiceRepository extends JpaRepository<ServiceEntity, Long> {

    @Query(value = """
                SELECT DISTINCT
                        s.id_servicio AS serviceId,
                        s.nombre_servicio AS serviceName,
                        s.descripcion_servicio AS serviceDescription,
                        CONCAT(u.nombres_usuario, ' ', u.apellidos_usuario) AS providerName
                FROM tbl_servicio s
                JOIN tbl_usuario u
                    ON u.id_usuario = s.id_usuario_proveedor
                JOIN tbl_disponibilidad_servicio ds
                    ON ds.id_servicio = s.id_servicio
                WHERE s.id_estado_servicio = (
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
                ORDER BY s.nombre_servicio
        """, nativeQuery = true)
    List<AvailableOfferProjection> findAvailableOffers();
}