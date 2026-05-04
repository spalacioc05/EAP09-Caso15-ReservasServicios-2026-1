package com.eap09.reservas.identityaccess.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

        @ParameterizedTest
        @MethodSource("blankFieldRequests")
        void shouldRejectWhenProvidedFieldIsBlank(UpdateOwnProfileRequest request, String expectedMessage) {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userProfileService.updateOwnProfile("cliente@reservas.test", request));
                assertEquals(expectedMessage, ex.getMessage());

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
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest(null, null, "correo-invalido");

        ApiException ex = assertThrows(ApiException.class,
                () -> userProfileService.updateOwnProfile("cliente@reservas.test", request));

        assertEquals("El correo ingresado no es valido", ex.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "cliente@reservas",
            "cliente@.test",
            "cliente@reservas.test.",
            "cliente @reservas.test",
            "cliente@@reservas.test",
            "cliente@"
    })
    void shouldRejectDuplicatedInvalidEmailCases(String invalidEmail) {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest(null, null, invalidEmail);

        ApiException ex = assertThrows(ApiException.class,
                () -> userProfileService.updateOwnProfile("cliente@reservas.test", request));

        assertEquals("El correo ingresado no es valido", ex.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
    }

    @Test
    void shouldRejectWhenPayloadDoesNotContainAnyFieldToUpdate() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest(null, null, null);

        ApiException ex = assertThrows(ApiException.class,
                () -> userProfileService.updateOwnProfile("cliente@reservas.test", request));

        assertEquals("Debe enviar al menos un campo para actualizar", ex.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
    }

    @Test
    void shouldUpdateOwnProfileWithoutChangingEmail() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest(null, "Cliente Actualizado", null);

        UpdateOwnProfileResponse response = userProfileService.updateOwnProfile("cliente@reservas.test", request);

        assertEquals("Ana", response.nombres());
        assertEquals("Cliente Actualizado", response.apellidos());
        assertEquals("cliente@reservas.test", response.correo());
        verify(userAccountRepository, never())
                .existsByCorreoUsuarioIgnoreCaseAndIdUsuarioNot(any(), any());
    }

    @Test
    void shouldPublishGenericFailureMessageWhenUnexpectedRuntimeExceptionOccurs() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        RuntimeException unexpectedException = new RuntimeException("boom");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenThrow(unexpectedException);
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest("Ana Maria", null, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userProfileService.updateOwnProfile("cliente@reservas.test", request));

        assertEquals("boom", ex.getMessage());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
        assertEquals("No fue posible completar la actualizacion del perfil", eventCaptor.getValue().details());
    }

    @Test
    void shouldRejectEmailAlreadyUsedByAnotherAccount() {
        UserAccountEntity user = buildUser(10L, "cliente@reservas.test", "Ana", "Cliente");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.existsByCorreoUsuarioIgnoreCaseAndIdUsuarioNot("duplicado@reservas.test", 10L))
                .thenReturn(true);
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest(null, null, "duplicado@reservas.test");

        EmailAlreadyRegisteredException ex = assertThrows(EmailAlreadyRegisteredException.class,
                () -> userProfileService.updateOwnProfile("cliente@reservas.test", request));
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
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest("Ana", "Cliente", "cliente@reservas.test");

        ProfileNoChangesException ex = assertThrows(ProfileNoChangesException.class,
                () -> userProfileService.updateOwnProfile("cliente@reservas.test", request));
        assertEquals("No existen cambios para aplicar", ex.getMessage());

        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectWhenAuthenticationIsMissing() {
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest("Ana", null, null);

        InsufficientAuthenticationException ex = assertThrows(InsufficientAuthenticationException.class,
                () -> userProfileService.updateOwnProfile("  ", request));
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

        private static Stream<Arguments> blankFieldRequests() {
                return Stream.of(
                                arguments(new UpdateOwnProfileRequest("   ", null, null), "nombres no puede estar vacio"),
                                arguments(new UpdateOwnProfileRequest(null, "   ", null), "apellidos no puede estar vacio"),
                                arguments(new UpdateOwnProfileRequest(null, null, "   "), "correo no puede estar vacio")
                );
        }
}