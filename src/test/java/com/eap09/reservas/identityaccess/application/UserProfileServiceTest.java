package com.eap09.reservas.identityaccess.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.common.exception.ProfileNoChangesException;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileRequest;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileResponse;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.InsufficientAuthenticationException;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void shouldUpdateOwnProfileSuccessfully() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.existsByCorreoUsuarioIgnoreCaseAndIdUsuarioNot("cliente.nuevo@reservas.test", 10L))
                .thenReturn(false);
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOwnProfileResponse response = userProfileService.updateOwnProfile(
                "cliente@reservas.test",
                new UpdateOwnProfileRequest("Ana Maria", null, "cliente.nuevo@reservas.test"));

        assertEquals(10L, response.idUsuario());
        assertEquals("Ana Maria", response.nombres());
        assertEquals("Cliente", response.apellidos());
        assertEquals("cliente.nuevo@reservas.test", response.correo());

        verify(userAccountRepository).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("ACTUALIZACION_PERFIL_USUARIO", eventCaptor.getValue().type());
        assertEquals("EXITO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectWhenProvidedFieldIsBlank() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userProfileService.updateOwnProfile(
                        "cliente@reservas.test",
                        new UpdateOwnProfileRequest("   ", null, null)));
        assertEquals("nombres no puede estar vacio", ex.getMessage());

        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userProfileService.updateOwnProfile(
                        "cliente@reservas.test",
                        new UpdateOwnProfileRequest(null, null, "correo-invalido")));

        assertEquals("El correo ingresado no es valido", ex.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectEmailAlreadyUsedByAnotherAccount() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.existsByCorreoUsuarioIgnoreCaseAndIdUsuarioNot("duplicado@reservas.test", 10L))
                .thenReturn(true);

        EmailAlreadyRegisteredException ex = assertThrows(EmailAlreadyRegisteredException.class,
                () -> userProfileService.updateOwnProfile(
                        "cliente@reservas.test",
                        new UpdateOwnProfileRequest(null, null, "duplicado@reservas.test")));
        assertEquals("El correo ingresado ya esta registrado", ex.getMessage());

        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectWhenThereAreNoRealChanges() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));

        ProfileNoChangesException ex = assertThrows(ProfileNoChangesException.class,
                () -> userProfileService.updateOwnProfile(
                        "cliente@reservas.test",
                        new UpdateOwnProfileRequest("Ana", "Cliente", "cliente@reservas.test")));
        assertEquals("No existen cambios para aplicar", ex.getMessage());

        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectWhenAuthenticationIsMissing() {
        InsufficientAuthenticationException ex = assertThrows(InsufficientAuthenticationException.class,
                () -> userProfileService.updateOwnProfile("  ", new UpdateOwnProfileRequest("Ana", null, null)));
        assertEquals("Autenticacion requerida", ex.getMessage());

        verify(userAccountRepository, never()).findByCorreoUsuarioIgnoreCase(any());
        verify(systemEventPublisher, never()).publish(any());
    }

    private UserAccountEntity buildUser(Long id, String correo, String nombres, String apellidos) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol("CLIENTE");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(id);
        user.setCorreoUsuario(correo);
        user.setNombresUsuario(nombres);
        user.setApellidosUsuario(apellidos);
        user.setRol(role);
        user.setIdEstado(1L);
        user.setIntentosFallidosConsecutivos(0);
        user.setHashContrasenaUsuario("hash");
        return user;
    }
}