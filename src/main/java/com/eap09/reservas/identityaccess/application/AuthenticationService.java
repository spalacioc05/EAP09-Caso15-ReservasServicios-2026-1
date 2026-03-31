package com.eap09.reservas.identityaccess.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AccountInactiveException;
import com.eap09.reservas.common.exception.InvalidCredentialsException;
import com.eap09.reservas.common.exception.TemporaryAccessRestrictedException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationRequest;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationResponse;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.config.SecurityProperties;
import com.eap09.reservas.security.domain.SecurityUserPrincipal;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int RESTRICTION_MINUTES = 15;
    private static final String USER_STATE_CATEGORY = "tbl_usuario";
    private static final String ACTIVE_STATE = "ACTIVA";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final SystemEventPublisher systemEventPublisher;

    public AuthenticationService(UserAccountRepository userAccountRepository,
                                 StateRepository stateRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 SecurityProperties securityProperties,
                                 SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
        this.systemEventPublisher = systemEventPublisher;
    }

    public AuthenticationResponse createSession(AuthenticationRequest request) {
        String normalizedEmail = request.correo().trim().toLowerCase();
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(normalizedEmail)
                .orElseThrow(() -> invalidCredentials(null, "Credenciales no validas"));

        if (isTemporarilyRestricted(user)) {
            publishAuthEvent(user, "FALLO", "Autenticacion rechazada por restriccion temporal");
            throw new TemporaryAccessRestrictedException("La cuenta tiene una restriccion temporal de acceso");
        }

        if (!isActive(user)) {
            publishAuthEvent(user, "FALLO", "Autenticacion rechazada por cuenta inactiva");
            throw new AccountInactiveException("La cuenta se encuentra inactiva");
        }

        if (!passwordEncoder.matches(request.contrasena(), user.getHashContrasenaUsuario())) {
            handleFailedAttempt(user);
            throw invalidCredentials(user, "Credenciales no validas");
        }

        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);
        userAccountRepository.save(user);

        SecurityUserPrincipal principal = new SecurityUserPrincipal(
                user.getIdUsuario(),
                user.getCorreoUsuario(),
                user.getHashContrasenaUsuario(),
                user.getRol().getNombreRol(),
                true,
                true
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRol().getNombreRol());
        String token = jwtService.generateToken(claims, principal);

        publishAuthEvent(user, "EXITO", "Autenticacion exitosa");

        return new AuthenticationResponse(
                token,
                "Bearer",
                securityProperties.getJwtExpirationSeconds(),
                user.getRol().getNombreRol()
        );
    }

    private InvalidCredentialsException invalidCredentials(UserAccountEntity user, String message) {
        publishAuthEvent(user, "FALLO", "Credenciales no validas");
        return new InvalidCredentialsException(message);
    }

    private void handleFailedAttempt(UserAccountEntity user) {
        int failedAttempts = user.getIntentosFallidosConsecutivos() == null
                ? 0
                : user.getIntentosFallidosConsecutivos();
        failedAttempts++;
        user.setIntentosFallidosConsecutivos(failedAttempts);

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime restrictionEndsAt = LocalDateTime.now().plusMinutes(RESTRICTION_MINUTES);
            user.setFechaFinRestriccionAcceso(restrictionEndsAt);
            publishRestrictionEvent(user, restrictionEndsAt);
        }

        userAccountRepository.save(user);
    }

    private boolean isTemporarilyRestricted(UserAccountEntity user) {
        LocalDateTime restrictionEndsAt = user.getFechaFinRestriccionAcceso();
        return restrictionEndsAt != null && restrictionEndsAt.isAfter(LocalDateTime.now());
    }

    private boolean isActive(UserAccountEntity user) {
        Long activeStateId = stateRepository.findByCategoryAndStateName(USER_STATE_CATEGORY, ACTIVE_STATE)
                .map(StateEntity::getIdEstado)
                .orElseGet(securityProperties::getUserActiveStateId);

        return activeStateId.equals(user.getIdEstado());
    }

    private void publishAuthEvent(UserAccountEntity user, String result, String details) {
        systemEventPublisher.publish(SystemEvent.now(
                "AUTENTICACION_USUARIO",
                "tbl_usuario",
                user == null ? null : String.valueOf(user.getIdUsuario()),
                result,
                details,
                TraceIdUtil.currentTraceId()));
    }

    private void publishRestrictionEvent(UserAccountEntity user, LocalDateTime restrictionEndsAt) {
        systemEventPublisher.publish(SystemEvent.now(
                "APLICACION_RESTRICCION_ACCESO",
                "tbl_usuario",
                String.valueOf(user.getIdUsuario()),
                "EXITO",
                "Restriccion temporal aplicada hasta " + restrictionEndsAt,
                TraceIdUtil.currentTraceId()));
    }
}
