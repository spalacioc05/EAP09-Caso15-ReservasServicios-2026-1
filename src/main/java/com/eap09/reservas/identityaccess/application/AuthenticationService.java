package com.eap09.reservas.identityaccess.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AccountInactiveException;
import com.eap09.reservas.common.exception.InvalidCredentialsException;
import com.eap09.reservas.common.exception.SessionNotActiveException;
import com.eap09.reservas.common.exception.TemporaryAccessRestrictedException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationRequest;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationResponse;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserSessionEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserSessionRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.config.SecurityProperties;
import com.eap09.reservas.security.domain.SecurityUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int RESTRICTION_MINUTES = 15;
    private static final String USER_STATE_CATEGORY = "tbl_usuario";
    private static final String ACTIVE_STATE = "ACTIVA";
    private static final String SESSION_STATE_CATEGORY = "tbl_sesion_usuario";
    private static final String SESSION_ACTIVE_STATE = "ACTIVA";
    private static final String SESSION_CLOSED_STATE = "CERRADA";
    private static final Pattern INET_LITERAL_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final SystemEventPublisher systemEventPublisher;

    public AuthenticationService(UserAccountRepository userAccountRepository,
                                 StateRepository stateRepository,
                                 UserSessionRepository userSessionRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 SecurityProperties securityProperties,
                                 SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.userSessionRepository = userSessionRepository;
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

        UUID sessionJti = UUID.randomUUID();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRol().getNombreRol());
        claims.put("jti", sessionJti.toString());
        String token = jwtService.generateToken(claims, principal);

        registerActiveSession(user.getIdUsuario(), sessionJti);

        publishAuthEvent(user, "EXITO", "Autenticacion exitosa");

        return new AuthenticationResponse(
                token,
                "Bearer",
                securityProperties.getJwtExpirationSeconds(),
                user.getRol().getNombreRol()
        );
    }

    public void closeCurrentSession(String accessToken, String authenticatedUsername) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new InsufficientAuthenticationException("Autenticacion requerida"));

        String tokenUsername;
        try {
            tokenUsername = jwtService.extractUsername(accessToken);
        } catch (RuntimeException ex) {
            throw new BadCredentialsException("Token invalido o expirado", ex);
        }

        if (tokenUsername == null || !tokenUsername.equalsIgnoreCase(authenticatedUsername)) {
            throw new BadCredentialsException("Token invalido para el usuario autenticado");
        }

        UUID sessionJti = parseSessionJti(accessToken);
        if (sessionJti == null) {
            publishLogoutEvent(user.getIdUsuario(), null, "FALLO", "Token sin identificador de sesion");
            throw new BadCredentialsException("Token invalido o expirado");
        }

        UserSessionEntity session = userSessionRepository.findByJtiTokenAndIdUsuario(sessionJti, user.getIdUsuario())
                .orElseThrow(() -> {
                    publishLogoutEvent(user.getIdUsuario(), null, "FALLO", "No existe sesion para el token solicitado");
                    return new SessionNotActiveException("No existe una sesion activa valida para cerrar");
                });

        Long activeSessionStateId = findStateId(SESSION_STATE_CATEGORY, SESSION_ACTIVE_STATE);
        if (!activeSessionStateId.equals(session.getIdEstadoSesion())) {
            publishLogoutEvent(user.getIdUsuario(), session.getIdSesionUsuario(), "FALLO", "La sesion asociada al token ya no esta activa");
            throw new SessionNotActiveException("No existe una sesion activa valida para cerrar");
        }

        Long closedSessionStateId = findStateId(SESSION_STATE_CATEGORY, SESSION_CLOSED_STATE);
        OffsetDateTime now = OffsetDateTime.now();
        session.setIdEstadoSesion(closedSessionStateId);
        session.setFechaCierreSesion(now);
        session.setFechaActualizacionSesion(now);
        userSessionRepository.save(session);

        publishLogoutEvent(user.getIdUsuario(), session.getIdSesionUsuario(), "EXITO", "Sesion cerrada de forma segura");
    }

    private InvalidCredentialsException invalidCredentials(UserAccountEntity user, String message) {
        publishAuthEvent(user, "FALLO", "Credenciales no validas");
        return new InvalidCredentialsException(message);
    }

    private void handleFailedAttempt(UserAccountEntity user) {
        Integer currentFailedAttempts = user.getIntentosFallidosConsecutivos();
        int failedAttempts = currentFailedAttempts == null
                ? 0
            : currentFailedAttempts;
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
        Long activeStateId = findStateId(USER_STATE_CATEGORY, ACTIVE_STATE, securityProperties.getUserActiveStateId());

        return activeStateId.equals(user.getIdEstado());
    }

    private void registerActiveSession(Long userId, UUID sessionJti) {
        Long activeSessionStateId = findStateId(SESSION_STATE_CATEGORY, SESSION_ACTIVE_STATE);

        OffsetDateTime now = OffsetDateTime.now();
        UserSessionEntity session = new UserSessionEntity();
        session.setIdUsuario(userId);
        session.setIdEstadoSesion(activeSessionStateId);
        session.setJtiToken(sessionJti);
        session.setFechaCreacionSesion(now);
        session.setFechaActualizacionSesion(now);
        session.setFechaExpiracionSesion(now.plusSeconds(securityProperties.getJwtExpirationSeconds()));
        session.setDireccionIp(resolveClientIp());
        session.setUserAgent(resolveUserAgent());

        userSessionRepository.save(session);
    }

    private Long findStateId(String categoryName, String stateName) {
        return findStateId(categoryName, stateName, null);
    }

    private Long findStateId(String categoryName, String stateName, Long fallback) {
        return stateRepository.findByCategoryAndStateName(categoryName, stateName)
                .map(StateEntity::getIdEstado)
                .orElseGet(() -> {
                    if (fallback == null) {
                        throw new IllegalStateException("No se encontro el estado requerido: " + categoryName + " / " + stateName);
                    }
                    return fallback;
                });
    }

    private String resolveUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getHeader("User-Agent");
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String forwardedIp = extractFirstForwardedIp(forwardedFor);

        if (forwardedIp != null) {
            return forwardedIp;
        }

        return sanitizeIpCandidate(request.getRemoteAddr());
    }

    private String extractFirstForwardedIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }

        String[] candidates = forwardedFor.split(",");
        for (String candidate : candidates) {
            String sanitized = sanitizeIpCandidate(candidate);
            if (sanitized != null) {
                return sanitized;
            }
        }

        return null;
    }

    private String sanitizeIpCandidate(String candidate) {
        if (candidate == null) {
            return null;
        }

        String sanitized = candidate.trim();
        if (sanitized.isBlank() || "unknown".equalsIgnoreCase(sanitized)) {
            return null;
        }

        if (sanitized.startsWith("[") && sanitized.endsWith("]") && sanitized.length() > 2) {
            sanitized = sanitized.substring(1, sanitized.length() - 1);
        }

        int zoneIndex = sanitized.indexOf('%');
        if (zoneIndex > 0) {
            sanitized = sanitized.substring(0, zoneIndex);
        }

        if (sanitized.contains(".") && sanitized.indexOf(':') == sanitized.lastIndexOf(':') && sanitized.indexOf(':') > 0) {
            sanitized = sanitized.substring(0, sanitized.indexOf(':'));
        }

        if (!INET_LITERAL_PATTERN.matcher(sanitized).matches()) {
            return null;
        }

        return sanitized;
    }

    private UUID parseSessionJti(String accessToken) {
        String tokenId = jwtService.extractTokenId(accessToken);
        if (tokenId == null || tokenId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tokenId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private void publishLogoutEvent(Long responsibleUserId, Long affectedSessionId, String result, String details) {
        systemEventPublisher.publish(SystemEvent.now(
                "CIERRE_SESION_USUARIO",
                "tbl_sesion_usuario",
                responsibleUserId == null ? null : String.valueOf(responsibleUserId),
                affectedSessionId == null ? null : String.valueOf(affectedSessionId),
                result,
                details,
                TraceIdUtil.currentTraceId()));
    }
}
