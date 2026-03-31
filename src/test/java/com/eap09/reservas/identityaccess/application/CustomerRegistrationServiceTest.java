package com.eap09.reservas.identityaccess.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.identityaccess.api.dto.CustomerRegistrationRequest;
import com.eap09.reservas.identityaccess.api.dto.CustomerRegistrationResponse;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerRegistrationServiceTest {

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

    @Test
    void shouldRegisterCustomerSuccessfully() {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "Ana",
                "Perez",
                "ana@example.com",
                "Password1!"
        );

        RoleEntity customerRole = new RoleEntity();
        customerRole.setIdRol(1L);
        customerRole.setNombreRol("CLIENTE");

        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(10L);
        activeState.setNombreEstado("ACTIVA");

        when(userAccountRepository.existsByCorreoUsuarioIgnoreCase("ana@example.com")).thenReturn(false);
        when(roleRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(customerRole));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(activeState));
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$hash");
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setIdUsuario(100L);
            return user;
        });

        CustomerRegistrationResponse response = customerRegistrationService.registerCustomer(request);

        assertEquals(100L, response.idUsuario());
        assertEquals("ana@example.com", response.correo());
        assertEquals("CLIENTE", response.rol());
        assertEquals("ACTIVA", response.estado());

        ArgumentCaptor<UserAccountEntity> captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(captor.capture());
        UserAccountEntity saved = captor.getValue();

        assertEquals("Ana", saved.getNombresUsuario());
        assertEquals("Perez", saved.getApellidosUsuario());
        assertEquals("ana@example.com", saved.getCorreoUsuario());
        assertEquals(10L, saved.getIdEstado());
        assertEquals("CLIENTE", saved.getRol().getNombreRol());
        assertNotEquals("Password1!", saved.getHashContrasenaUsuario());
        assertEquals("$2a$hash", saved.getHashContrasenaUsuario());

        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "Ana",
                "Perez",
                "ana@example.com",
                "Password1!"
        );

        when(userAccountRepository.existsByCorreoUsuarioIgnoreCase("ana@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> customerRegistrationService.registerCustomer(request));

        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
        verify(systemEventPublisher, never()).publish(any());
    }
}
