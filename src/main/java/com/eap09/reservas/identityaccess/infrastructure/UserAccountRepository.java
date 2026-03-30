package com.eap09.reservas.identityaccess.infrastructure;

import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

    @EntityGraph(attributePaths = "rol")
    Optional<UserAccountEntity> findByCorreoUsuarioIgnoreCase(String correoUsuario);
}
