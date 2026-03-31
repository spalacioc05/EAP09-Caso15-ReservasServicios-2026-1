package com.eap09.reservas.identityaccess.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AccountInactiveException;
import com.eap09.reservas.common.exception.InvalidCredentialsException;
import com.eap09.reservas.common.exception.TemporaryAccessRestrictedException;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationRequest;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationResponse;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateCategoryEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.config.SecurityProperties;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

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

    @Test
    void shouldAuthenticateSuccessfullyAndResetFailedAttempts() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
            .thenReturn(Optional.of(activeState()));
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
            .thenReturn(Optional.of(activeState()));

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
            .thenReturn(Optional.of(activeState()));

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
            .thenReturn(Optional.of(activeState()));

        UserAccountEntity user = buildUser("user@example.com", "CLIENTE", 2L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(AccountInactiveException.class,
                () -> authenticationService.createSession(new AuthenticationRequest("user@example.com", "Password1!")));
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

    private StateEntity activeState() {
        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(1L);
        activeState.setNombreEstado("ACTIVA");

        StateCategoryEntity category = new StateCategoryEntity();
        category.setNombreCategoriaEstado("tbl_usuario");
        activeState.setCategoriaEstado(category);

        return activeState;
    }
}
