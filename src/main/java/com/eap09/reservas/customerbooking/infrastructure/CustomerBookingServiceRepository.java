package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerBookingServiceRepository extends JpaRepository<ServiceEntity, Long> {

    @Query("""
        SELECT new com.eap09.reservas.customerbooking.api.dto.OfferResponse(
            s.idServicio,
            s.nombreServicio,
            s.descripcionServicio,
            CONCAT(u.nombresUsuario, ' ', u.apellidosUsuario)
        )
        FROM ServiceEntity s
        JOIN com.eap09.reservas.identityaccess.domain.UserAccountEntity u
            ON s.idUsuarioProveedor = u.idUsuario
        WHERE s.idEstadoServicio = 3
            AND u.idEstado = 1
    """)
    List<OfferResponse> findAvailableOffers();
}