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
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationRequest;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationResponse;
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
class ProviderRegistrationServiceTest {

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

    @Test
    void shouldRegisterProviderSuccessfully() {
        ProviderRegistrationRequest request = new ProviderRegistrationRequest(
                "Pablo",
                "Rojas",
                "Pablo@Example.com",
                "Password1!"
        );

        RoleEntity providerRole = new RoleEntity();
        providerRole.setIdRol(2L);
        providerRole.setNombreRol("PROVEEDOR");

        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(10L);
        activeState.setNombreEstado("ACTIVA");

        when(userAccountRepository.existsByCorreoUsuarioIgnoreCase("pablo@example.com")).thenReturn(false);
        when(roleRepository.findByNombreRol("PROVEEDOR")).thenReturn(Optional.of(providerRole));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(activeState));
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$hash");
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setIdUsuario(300L);
            return user;
        });

        ProviderRegistrationResponse response = providerRegistrationService.registerProvider(request);

        assertEquals(300L, response.idUsuario());
        assertEquals("pablo@example.com", response.correo());
        assertEquals("PROVEEDOR", response.rol());
        assertEquals("ACTIVA", response.estado());

        ArgumentCaptor<UserAccountEntity> captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(captor.capture());
        UserAccountEntity saved = captor.getValue();

        assertEquals("Pablo", saved.getNombresUsuario());
        assertEquals("Rojas", saved.getApellidosUsuario());
        assertEquals("pablo@example.com", saved.getCorreoUsuario());
        assertEquals(10L, saved.getIdEstado());
        assertEquals("PROVEEDOR", saved.getRol().getNombreRol());
        assertNotEquals("Password1!", saved.getHashContrasenaUsuario());
        assertEquals("$2a$hash", saved.getHashContrasenaUsuario());

        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        ProviderRegistrationRequest request = new ProviderRegistrationRequest(
                "Pablo",
                "Rojas",
                "pablo@example.com",
                "Password1!"
        );

        when(userAccountRepository.existsByCorreoUsuarioIgnoreCase("pablo@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> providerRegistrationService.registerProvider(request));

        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
        verify(systemEventPublisher, never()).publish(any());
    }
}
