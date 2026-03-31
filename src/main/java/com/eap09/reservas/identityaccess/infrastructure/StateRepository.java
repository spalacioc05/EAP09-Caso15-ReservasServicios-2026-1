package com.eap09.reservas.identityaccess.infrastructure;

import com.eap09.reservas.identityaccess.domain.StateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StateRepository extends JpaRepository<StateEntity, Long> {

    @Query("""
            SELECT e
            FROM StateEntity e
            JOIN e.categoriaEstado c
            WHERE c.nombreCategoriaEstado = :categoryName
              AND e.nombreEstado = :stateName
            """)
    Optional<StateEntity> findByCategoryAndStateName(@Param("categoryName") String categoryName,
                                                     @Param("stateName") String stateName);
}
