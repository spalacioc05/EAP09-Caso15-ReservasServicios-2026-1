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
import com.eap09.reservas.identityaccess.api.dto.CustomerRegistrationRequest;
import com.eap09.reservas.identityaccess.api.dto.CustomerRegistrationResponse;
import com.eap09.reservas.identityaccess.application.CustomerRegistrationService;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;

public class CustomerRegistrationServiceTest {

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
    private CustomerRegistrationService customerRegistrationService;

    private CustomerRegistrationRequest request;
    private StateEntity state;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        request = new CustomerRegistrationRequest(
                "Salome",
                "Giraldo",
                "salome.giraldo@gmail.com",
                "Salome098!");

        state = new StateEntity();
        state.setIdEstado(1L);
        state.setNombreEstado("ACTIVA");

        role = new RoleEntity();
        role.setIdRol(1L);
        role.setNombreRol("CLIENTE");
    }

    @Test
    void customerRegistration_Success() {
        when(roleRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(role));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(state));
        when(passwordEncoder.encode("Salome098!")).thenReturn("$2a$hash");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(100L);
        user.setApellidosUsuario("Giraldo");
        user.setNombresUsuario("Salome");
        user.setCorreoUsuario("salome.giraldo@gmail.com");
        user.setHashContrasenaUsuario("$2a$hash");
        user.setRol(role);
        user.setIdEstado(state.getIdEstado());

        when(userAccountRepository.save(any(UserAccountEntity.class))).thenReturn(user);

        CustomerRegistrationResponse response = customerRegistrationService.registerCustomer(request);

        assertEquals(100L, response.idUsuario());
        assertEquals("salome.giraldo@gmail.com", response.correo());
        assertEquals("CLIENTE", response.rol());
        assertEquals("ACTIVA", response.estado());

    };

    @Test
    void DuplicateEmail_Exception() {
        when(userAccountRepository.existsByCorreoUsuarioIgnoreCase("salome.giraldo@gmail.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> customerRegistrationService.registerCustomer(request));
    }

    @Test
    void EmptyPassword_Exception() {
        request = new CustomerRegistrationRequest(
                "Salome",
                "Giraldo",
                "salome.giraldo@gmail.com",
                "");

        assertThrows(UnsupportedOperationException.class,
                () -> customerRegistrationService.registerCustomer(request));
    }

    @Test
    void IncorrectPassword_Exception() {
        when(roleRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(role));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(state));
        when(passwordEncoder.encode("salome")).thenReturn("$2hash");

        assertThrows(UnsupportedOperationException.class,
                () -> customerRegistrationService.registerCustomer(request));
    }

};
