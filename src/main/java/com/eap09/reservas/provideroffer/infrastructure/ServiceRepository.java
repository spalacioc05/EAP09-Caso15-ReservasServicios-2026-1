package com.eap09.reservas.provideroffer.infrastructure;

import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    @Query("""
            SELECT COUNT(s) > 0
            FROM ServiceEntity s
            WHERE s.idUsuarioProveedor = :providerUserId
              AND LOWER(TRIM(s.nombreServicio)) = LOWER(TRIM(:serviceName))
            """)
    boolean existsByProviderAndServiceName(@Param("providerUserId") Long providerUserId,
                                           @Param("serviceName") String serviceName);
}