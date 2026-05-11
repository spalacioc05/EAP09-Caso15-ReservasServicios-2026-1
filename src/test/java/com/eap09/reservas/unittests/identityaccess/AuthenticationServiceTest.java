package com.eap09.reservas.unittests.identityaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AccountInactiveException;
import com.eap09.reservas.common.exception.InvalidCredentialsException;
import com.eap09.reservas.common.exception.SessionNotActiveException;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationRequest;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationResponse;
import com.eap09.reservas.identityaccess.application.AuthenticationService;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.domain.UserSessionEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserSessionRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.config.SecurityProperties;

public class AuthenticationServiceTest {

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

    private AuthenticationRequest request;
    private StateEntity userActiveState;
    private StateEntity sessionActiveState;
    private StateEntity sessionClosedState;
    private RoleEntity role;
    private UserAccountEntity user;
    private UserSessionEntity session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        request = new AuthenticationRequest("camila@udea.edu.co", "Camila123$");

        userActiveState = new StateEntity();
        userActiveState.setIdEstado(1L);
        userActiveState.setNombreEstado("ACTIVA");

        sessionActiveState = new StateEntity();
        sessionActiveState.setIdEstado(10L);
        sessionActiveState.setNombreEstado("ACTIVA");

        role = new RoleEntity();
        role.setIdRol(1L);
        role.setNombreRol("CLIENTE");

        user = new UserAccountEntity();
        user.setIdUsuario(1L);
        user.setCorreoUsuario("camila@udea.edu.co");
        user.setHashContrasenaUsuario("$2a$hash");
        user.setRol(role);
        user.setIdEstado(1L);
        user.setIntentosFallidosConsecutivos(0);

        session = new UserSessionEntity();
        session.setIdSesionUsuario(100L);
        session.setIdEstadoSesion(10L);
        session.setIdUsuario(1L);

        sessionClosedState = new StateEntity();
        sessionClosedState.setIdEstado(11L);
        sessionClosedState.setNombreEstado("CERRADA");
    }

    @Test
    void createSession_Success() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(userActiveState));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(sessionActiveState));
        when(securityProperties.getJwtExpirationSeconds()).thenReturn(1800L);
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("camila@udea.edu.co"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Camila123$", "$2a$hash")).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.createSession(request);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1800L, response.expiresIn());
        assertEquals("CLIENTE", response.role());
    }

    @Test
    void createSession_UnknownEmail_Exception() {
        request = new AuthenticationRequest("sebas@gmail.com", "Sebastian1!");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("sebas@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.createSession(request));
    }

    @Test
    void createSession_WrongPassword_Exception() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(userActiveState));
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("camila@gmail.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Camila191!", "$2a$hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.createSession(request));
    }

    @Test
    void createSession_InactiveAccount_Exception() {
        user.setIdEstado(2L);
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(userActiveState));
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("camila@gmail.com"))
                .thenReturn(Optional.of(user));

        assertThrows(AccountInactiveException.class,
                () -> authenticationService.createSession(request));
    }

    @Test
    void EmptyPassword_Exception() {
        request = new AuthenticationRequest("camila@udea.edu.co", "");

        assertThrows(UnsupportedOperationException.class,
                () -> authenticationService.createSession(request));
    }

    @Test
    void closeCurrentSession_Success() {
        java.util.UUID jti = java.util.UUID.randomUUID();
        when(jwtService.extractUsername("token")).thenReturn("camila@udea.edu.co");
        when(jwtService.extractTokenId("token")).thenReturn(jti.toString());
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("camila@udea.edu.co")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByJtiTokenAndIdUsuario(jti, 1L)).thenReturn(Optional.of(session));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(sessionActiveState));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "CERRADA"))
                .thenReturn(Optional.of(sessionClosedState));

        authenticationService.closeCurrentSession("token", "camila@udea.edu.co");

        assertEquals(11L, session.getIdEstadoSesion());
    }

    @Test
    void closeCurrentSession_NotActive_Exception() {
        java.util.UUID jti = java.util.UUID.randomUUID();
        session.setIdEstadoSesion(11L);

        when(jwtService.extractUsername("token")).thenReturn("camila@udea.edu.co");
        when(jwtService.extractTokenId("token")).thenReturn(jti.toString());
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("camila@udea.edu.co")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByJtiTokenAndIdUsuario(jti, 1L)).thenReturn(Optional.of(session));
        when(stateRepository.findByCategoryAndStateName("tbl_sesion_usuario", "ACTIVA"))
                .thenReturn(Optional.of(sessionActiveState));

        assertThrows(SessionNotActiveException.class,
                () -> authenticationService.closeCurrentSession("token", "camila@udea.edu.co"));
    }
}
