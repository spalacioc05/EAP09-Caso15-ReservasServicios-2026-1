package com.eap09.reservas.identityaccess.infrastructure;

import com.eap09.reservas.identityaccess.domain.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByNombreRol(String nombreRol);
}
