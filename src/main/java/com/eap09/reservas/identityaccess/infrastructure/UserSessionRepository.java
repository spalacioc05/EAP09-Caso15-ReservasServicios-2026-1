package com.eap09.reservas.identityaccess.infrastructure;

import com.eap09.reservas.identityaccess.domain.UserSessionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    Optional<UserSessionEntity> findByJtiToken(UUID jtiToken);

    Optional<UserSessionEntity> findByJtiTokenAndIdUsuario(UUID jtiToken, Long idUsuario);

    boolean existsByJtiTokenAndIdEstadoSesion(UUID jtiToken, Long idEstadoSesion);
}
