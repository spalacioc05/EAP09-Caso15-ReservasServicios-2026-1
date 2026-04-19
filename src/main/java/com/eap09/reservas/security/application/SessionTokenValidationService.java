package com.eap09.reservas.security.application;

import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionTokenValidationService {

    private static final String SESSION_STATE_CATEGORY = "tbl_sesion_usuario";
    private static final String SESSION_ACTIVE_STATE = "ACTIVA";

    private final UserSessionRepository userSessionRepository;
    private final StateRepository stateRepository;

    public SessionTokenValidationService(UserSessionRepository userSessionRepository,
                                         StateRepository stateRepository) {
        this.userSessionRepository = userSessionRepository;
        this.stateRepository = stateRepository;
    }

    public boolean isActiveSessionToken(String tokenId) {
        UUID jti = parseUuid(tokenId);
        if (jti == null) {
            return false;
        }

        Optional<Long> activeStateId = stateRepository.findByCategoryAndStateName(
                SESSION_STATE_CATEGORY,
                SESSION_ACTIVE_STATE).map(StateEntity::getIdEstado);

        return activeStateId.isPresent() && userSessionRepository.existsByJtiTokenAndIdEstadoSesion(jti, activeStateId.get());
    }

    private UUID parseUuid(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tokenId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
