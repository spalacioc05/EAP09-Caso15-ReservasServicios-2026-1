package com.eap09.reservas.identityaccess.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AccountInactiveException;
import com.eap09.reservas.common.exception.InvalidCredentialsException;
import com.eap09.reservas.common.exception.SessionNotActiveException;
import com.eap09.reservas.common.exception.TemporaryAccessRestrictedException;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationRequest;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationResponse;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateCategoryEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserSessionEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserSessionRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.config.SecurityProperties;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private AuthenticationService authenticationService;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldAuthenticateSuccessfullyAndResetFailedAttempts() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_usuario", "ACTIVA", 1L)));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_sesion_usuario", "ACTIVA", 10L)));
        when(securityProperties.getJwtExpirationSeconds()).thenReturn(1800L);

        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        user.setIntentosFallidosConsecutivos(3);
        user.setFechaFinRestriccionAcceso(LocalDateTime.now().minusMinutes(1));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hash")).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.createSession(
                new AuthenticationRequest("User@Example.com", "Password1!"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1800L, response.expiresIn());
        assertEquals("CLIENTE", response.role());

        ArgumentCaptor<UserAccountEntity> captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository, atLeastOnce()).save(captor.capture());
        UserAccountEntity saved = captor.getValue();
        assertEquals(0, saved.getIntentosFallidosConsecutivos());
        assertEquals(null, saved.getFechaFinRestriccionAcceso());
        verify(userSessionRepository).save(any(UserSessionEntity.class));
    }

    @Test
    void shouldPersistFirstForwardedIpWhenCreatingSession() {
        prepareRequestContext("203.0.113.11, 10.0.0.1", "198.51.100.80", "JUnit");

        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_usuario", "ACTIVA", 1L)));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_sesion_usuario", "ACTIVA", 10L)));
        when(securityProperties.getJwtExpirationSeconds()).thenReturn(1800L);

        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hash")).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        authenticationService.createSession(new AuthenticationRequest("user@example.com", "Password1!"));

        ArgumentCaptor<UserSessionEntity> sessionCaptor = ArgumentCaptor.forClass(UserSessionEntity.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        assertEquals("203.0.113.11", sessionCaptor.getValue().getDireccionIp());
    }

    @Test
    void shouldPersistRemoteAddressWhenForwardedHeaderIsMissing() {
        prepareRequestContext(null, "198.51.100.90", "JUnit");

        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_usuario", "ACTIVA", 1L)));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_sesion_usuario", "ACTIVA", 10L)));
        when(securityProperties.getJwtExpirationSeconds()).thenReturn(1800L);

        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hash")).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        authenticationService.createSession(new AuthenticationRequest("user@example.com", "Password1!"));

        ArgumentCaptor<UserSessionEntity> sessionCaptor = ArgumentCaptor.forClass(UserSessionEntity.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        assertEquals("198.51.100.90", sessionCaptor.getValue().getDireccionIp());
    }

    @Test
    void shouldRejectUnknownEmailWithInvalidCredentials() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.createSession(new AuthenticationRequest("unknown@example.com", "Password1!")));

        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldRejectWrongPasswordAndIncreaseFailedAttempts() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_usuario", "ACTIVA", 1L)));

        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        user.setIntentosFallidosConsecutivos(2);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.createSession(new AuthenticationRequest("user@example.com", "wrong")));

        ArgumentCaptor<UserAccountEntity> captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getIntentosFallidosConsecutivos());
    }

    @Test
    void shouldApplyRestrictionAfterFifthFailedAttempt() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_usuario", "ACTIVA", 1L)));

        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        user.setIntentosFallidosConsecutivos(4);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.createSession(new AuthenticationRequest("user@example.com", "wrong")));

        ArgumentCaptor<UserAccountEntity> captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(captor.capture());
        UserAccountEntity saved = captor.getValue();
        assertEquals(5, saved.getIntentosFallidosConsecutivos());
        assertNotNull(saved.getFechaFinRestriccionAcceso());
        verify(systemEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void shouldRejectTemporarilyRestrictedAccount() {
        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        user.setFechaFinRestriccionAcceso(LocalDateTime.now().plusMinutes(5));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(TemporaryAccessRestrictedException.class,
                () -> authenticationService.createSession(new AuthenticationRequest("user@example.com", "Password1!")));
    }

    @Test
    void shouldRejectInactiveAccount() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_usuario", "ACTIVA", 1L)));

        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 2L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(AccountInactiveException.class,
                () -> authenticationService.createSession(new AuthenticationRequest("user@example.com", "Password1!")));
    }

    @Test
    void shouldCloseActiveSessionSuccessfully() {
        UUID jti = UUID.randomUUID();
        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        UserSessionEntity session = new UserSessionEntity();
        session.setIdSesionUsuario(99L);
        session.setIdUsuario(user.getIdUsuario());
        session.setIdEstadoSesion(10L);
        session.setJtiToken(jti);
        session.setFechaCreacionSesion(OffsetDateTime.now().minusMinutes(5));
        session.setFechaExpiracionSesion(OffsetDateTime.now().plusMinutes(25));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.extractUsername("token")).thenReturn("user@example.com");
        when(jwtService.extractTokenId("token")).thenReturn(jti.toString());
        when(userSessionRepository.findByJtiTokenAndIdUsuario(jti, user.getIdUsuario())).thenReturn(Optional.of(session));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_sesion_usuario", "ACTIVA", 10L)));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "CERRADA"))
                .thenReturn(Optional.of(state("tbl_sesion_usuario", "CERRADA", 11L)));

        authenticationService.closeCurrentSession("token", "user@example.com");

        verify(userSessionRepository).save(any(UserSessionEntity.class));
        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher, atLeastOnce()).publish(eventCaptor.capture());

        SystemEvent logoutEvent = eventCaptor.getAllValues().stream()
                .filter(event -> "CIERRE_SESION_USUARIO".equals(event.type()))
                .findFirst()
                .orElseThrow();

        assertEquals(String.valueOf(user.getIdUsuario()), logoutEvent.responsibleUserId());
        assertEquals(String.valueOf(session.getIdSesionUsuario()), logoutEvent.entityId());
    }

    @Test
    void shouldRejectLogoutWhenSessionIsNotActive() {
        UUID jti = UUID.randomUUID();
        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);
        UserSessionEntity session = new UserSessionEntity();
        session.setIdSesionUsuario(99L);
        session.setIdUsuario(user.getIdUsuario());
        session.setIdEstadoSesion(11L);
        session.setJtiToken(jti);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.extractUsername("token")).thenReturn("user@example.com");
        when(jwtService.extractTokenId("token")).thenReturn(jti.toString());
        when(userSessionRepository.findByJtiTokenAndIdUsuario(jti, user.getIdUsuario())).thenReturn(Optional.of(session));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(state("tbl_sesion_usuario", "ACTIVA", 10L)));

        assertThrows(SessionNotActiveException.class,
                () -> authenticationService.closeCurrentSession("token", "user@example.com"));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher, atLeastOnce()).publish(eventCaptor.capture());

        SystemEvent logoutEvent = eventCaptor.getAllValues().stream()
                .filter(event -> "CIERRE_SESION_USUARIO".equals(event.type()))
                .findFirst()
                .orElseThrow();

        assertEquals(String.valueOf(user.getIdUsuario()), logoutEvent.responsibleUserId());
        assertEquals(String.valueOf(session.getIdSesionUsuario()), logoutEvent.entityId());
    }

    @Test
    void shouldRejectLogoutWhenAuthenticationIsMissing() {
        assertThrows(InsufficientAuthenticationException.class,
                () -> authenticationService.closeCurrentSession("token", "  "));
    }

    @Test
    void shouldRejectLogoutWhenTokenIsInvalid() {
        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.extractUsername("token")).thenThrow(new RuntimeException("invalid token"));

        assertThrows(BadCredentialsException.class,
                () -> authenticationService.closeCurrentSession("token", "user@example.com"));
    }

    @Test
    void shouldRejectLogoutWhenSessionDoesNotExist() {
        UUID jti = UUID.randomUUID();
        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 1L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.extractUsername("token")).thenReturn("user@example.com");
        when(jwtService.extractTokenId("token")).thenReturn(jti.toString());
        when(userSessionRepository.findByJtiTokenAndIdUsuario(jti, user.getIdUsuario())).thenReturn(Optional.empty());

        assertThrows(SessionNotActiveException.class,
                () -> authenticationService.closeCurrentSession("token", "user@example.com"));

        verify(systemEventPublisher, atLeastOnce()).publish(any());
    }

    private void prepareRequestContext(String forwardedFor, String remoteAddr, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        request.setRemoteAddr(remoteAddr);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private UserAccountEntity buildUser(String email, String roleName, Long stateId) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol(roleName);

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(1L);
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario("hash");
        user.setRol(role);
        user.setIdEstado(stateId);
        user.setIntentosFallidosConsecutivos(0);
        return user;
    }

    private StateEntity state(String categoryName, String stateName, Long stateId) {
        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(stateId);
        activeState.setNombreEstado(stateName);

        StateCategoryEntity category = new StateCategoryEntity();
        category.setNombreCategoriaEstado(categoryName);
        activeState.setCategoriaEstado(category);

        return activeState;
    }
}
