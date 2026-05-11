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
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationRequest;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationResponse;
import com.eap09.reservas.identityaccess.application.ProviderRegistrationService;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;

public class ProviderRegistrationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private ProviderRegistrationService providerRegistrationService;

    private ProviderRegistrationRequest request;
    private StateEntity state;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        request = new ProviderRegistrationRequest(
                "Camilo",
                "Lopez",
                "camilo.lopez@gmail.com",
                "Camilo123!");

        state = new StateEntity();
        state.setIdEstado(1L);
        state.setNombreEstado("ACTIVA");

        role = new RoleEntity();
        role.setIdRol(1L);
        role.setNombreRol("PROVEEDOR");
    }

    @Test
    void providerRegistration_Success() {
        when(roleRepository.findByNombreRol("PROVEEDOR")).thenReturn(Optional.of(role));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(state));
        when(passwordEncoder.encode("Camilo123!")).thenReturn("$2a$hash");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(102L);
        user.setApellidosUsuario("Lopez");
        user.setNombresUsuario("Camilo");
        user.setCorreoUsuario("camilo.lopez@gmail.com");
        user.setHashContrasenaUsuario("$2a$hash");
        user.setRol(role);
        user.setIdEstado(state.getIdEstado());

        when(userAccountRepository.save(any(UserAccountEntity.class))).thenReturn(user);

        ProviderRegistrationResponse response = providerRegistrationService.registerProvider(request);

        assertEquals(102L, response.idUsuario());
        assertEquals("camilo.lopez@gmail.com", response.correo());
        assertEquals("PROVEEDOR", response.rol());
        assertEquals("ACTIVA", response.estado());

    };

    @Test
    void DuplicateEmail_Exception() {
        when(userAccountRepository.existsByCorreoUsuarioIgnoreCase("salome.giraldo@gmail.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> providerRegistrationService.registerProvider(request));
    }

    @Test
    void EmptyPassword_Exception() {
        request = new ProviderRegistrationRequest(
                "Camilo",
                "Lopez",
                "camilo.lopez@gmail.com",
                "");

        assertThrows(UnsupportedOperationException.class,
                () -> providerRegistrationService.registerProvider(request));
    }

    @Test
    void IncorrectPassword_Exception() {
        when(roleRepository.findByNombreRol("PROVEEDOR")).thenReturn(Optional.of(role));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(state));
        when(passwordEncoder.encode("camilo")).thenReturn("$2hash");

        assertThrows(UnsupportedOperationException.class,
                () -> providerRegistrationService.registerProvider(request));
    }

}
