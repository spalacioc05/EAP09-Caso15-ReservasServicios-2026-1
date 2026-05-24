package com.eap09.reservas.administration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.administration.api.dto.UserRoleUpdateRequest;
import com.eap09.reservas.administration.api.dto.UserRoleUpdateResponse;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.TargetUserInactiveException;
import com.eap09.reservas.common.exception.UserRoleAlreadyAssignedException;
import com.eap09.reservas.common.exception.UserRoleUpdateFailedException;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

@ExtendWith(MockitoExtension.class)
class UserRoleAdministrationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private UserRoleAdministrationService userRoleAdministrationService;

    @Test
    void shouldUpdateTargetUserRoleSuccessfully() {
        UserAccountEntity adminUser = buildUser(1L, "admin@reservas.test", "ADMINISTRADOR", 100L);
        UserAccountEntity targetUser = buildUser(20L, "cliente@reservas.test", "CLIENTE", 100L);
        RoleEntity providerRole = buildRole(2L, "PROVEEDOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser));
        when(roleRepository.findByNombreRol("PROVEEDOR"))
                .thenReturn(Optional.of(providerRole));
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(activeUserState()));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserRoleUpdateResponse response = userRoleAdministrationService.updateUserRole(
                "admin@reservas.test",
                20L,
                new UserRoleUpdateRequest("proveedor"));

        assertEquals(20L, response.idUsuario());
        assertEquals("cliente@reservas.test", response.correo());
        assertEquals("CLIENTE", response.rolAnterior());
        assertEquals("PROVEEDOR", response.rolActual());
        assertEquals("PROVEEDOR", targetUser.getRol().getNombreRol());
        assertEquals("cliente@reservas.test", targetUser.getCorreoUsuario());
        assertEquals(100L, targetUser.getIdEstado());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("ACTUALIZACION_ROL_USUARIO", eventCaptor.getValue().type());
        assertEquals("EXITO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() {
        UserAccountEntity providerUser = buildUser(1L, "provider@reservas.test", "PROVEEDOR", 100L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@reservas.test"))
                .thenReturn(Optional.of(providerUser));

        AdminRoleRequiredException exception = assertThrows(
                AdminRoleRequiredException.class,
                () -> userRoleAdministrationService.updateUserRole(
                        "provider@reservas.test",
                        20L,
                        new UserRoleUpdateRequest("CLIENTE")));

        assertEquals("Solo un administrador autenticado puede actualizar roles de usuario", exception.getMessage());
        verify(roleRepository, never()).findByNombreRol(any());
        verify(systemEventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectWhenTargetUserAlreadyHasRequestedRole() {
        UserAccountEntity adminUser = buildUser(1L, "admin@reservas.test", "ADMINISTRADOR", 100L);
        UserAccountEntity targetUser = buildUser(20L, "proveedor@reservas.test", "PROVEEDOR", 100L);
        RoleEntity providerRole = buildRole(2L, "PROVEEDOR");
        UserRoleUpdateRequest request = new UserRoleUpdateRequest("PROVEEDOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser));
        when(roleRepository.findByNombreRol("PROVEEDOR"))
                .thenReturn(Optional.of(providerRole));
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(activeUserState()));

        UserRoleAlreadyAssignedException exception = assertThrows(
                UserRoleAlreadyAssignedException.class,
                () -> userRoleAdministrationService.updateUserRole("admin@reservas.test", 20L, request));

        assertEquals("El usuario ya tiene asignado ese rol", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
        assertEquals("El usuario ya tiene asignado ese rol", eventCaptor.getValue().details());
    }

    @Test
    void shouldRejectWhenTargetUserIsInactive() {
        UserAccountEntity adminUser = buildUser(1L, "admin@reservas.test", "ADMINISTRADOR", 100L);
        UserAccountEntity targetUser = buildUser(20L, "cliente@reservas.test", "CLIENTE", 200L);
        RoleEntity providerRole = buildRole(2L, "PROVEEDOR");
        UserRoleUpdateRequest request = new UserRoleUpdateRequest("PROVEEDOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser));
        when(roleRepository.findByNombreRol("PROVEEDOR"))
                .thenReturn(Optional.of(providerRole));
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(activeUserState()));

        TargetUserInactiveException exception = assertThrows(
                TargetUserInactiveException.class,
                () -> userRoleAdministrationService.updateUserRole("admin@reservas.test", 20L, request));

        assertEquals("El usuario se encuentra desactivado", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
        assertEquals("El usuario se encuentra desactivado", eventCaptor.getValue().details());
    }

    @Test
    void shouldRejectWhenTargetUserDoesNotExist() {
        UserAccountEntity adminUser = buildUser(1L, "admin@reservas.test", "ADMINISTRADOR", 100L);
        RoleEntity providerRole = buildRole(2L, "PROVEEDOR");
        UserRoleUpdateRequest request = new UserRoleUpdateRequest("PROVEEDOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser));
        when(roleRepository.findByNombreRol("PROVEEDOR"))
                .thenReturn(Optional.of(providerRole));
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userRoleAdministrationService.updateUserRole("admin@reservas.test", 20L, request));

        assertEquals("El usuario indicado no existe", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenRequestedRoleDoesNotExist() {
        UserAccountEntity adminUser = buildUser(1L, "admin@reservas.test", "ADMINISTRADOR", 100L);
        UserRoleUpdateRequest request = new UserRoleUpdateRequest("SUPERVISOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser));
        when(roleRepository.findByNombreRol("SUPERVISOR"))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> userRoleAdministrationService.updateUserRole("admin@reservas.test", 20L, request));

        assertEquals("ROLE_NAME_INVALID", exception.getErrorCode());
        assertEquals("El rol indicado no existe", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenAuthenticationIsMissing() {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest("CLIENTE");

        InsufficientAuthenticationException exception = assertThrows(
                InsufficientAuthenticationException.class,
                () -> userRoleAdministrationService.updateUserRole(" ", 20L, request));

        assertEquals("Autenticacion requerida", exception.getMessage());
        verify(userAccountRepository, never()).findByCorreoUsuarioIgnoreCase(any());
        verify(systemEventPublisher, never()).publish(any());
    }

    @Test
    void shouldTranslateUnexpectedDataAccessFailureIntoControlledException() {
        UserAccountEntity adminUser = buildUser(1L, "admin@reservas.test", "ADMINISTRADOR", 100L);
        UserAccountEntity targetUser = buildUser(20L, "cliente@reservas.test", "CLIENTE", 100L);
        RoleEntity providerRole = buildRole(2L, "PROVEEDOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser));
        when(roleRepository.findByNombreRol("PROVEEDOR"))
                .thenReturn(Optional.of(providerRole));
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(activeUserState()));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));

        UserRoleUpdateFailedException exception = assertThrows(
                UserRoleUpdateFailedException.class,
                () -> userRoleAdministrationService.updateUserRole(
                        "admin@reservas.test",
                        20L,
                        new UserRoleUpdateRequest("PROVEEDOR")));

        assertEquals(
                "No fue posible completar la actualizacion del rol del usuario. Intenta nuevamente mas tarde",
                exception.getMessage());
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    private UserAccountEntity buildUser(Long idUsuario, String correo, String nombreRol, Long idEstado) {
        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(idUsuario);
        user.setCorreoUsuario(correo);
        user.setRol(buildRole(1L, nombreRol));
        user.setIdEstado(idEstado);
        return user;
    }

    private RoleEntity buildRole(Long idRol, String nombreRol) {
        RoleEntity role = new RoleEntity();
        role.setIdRol(idRol);
        role.setNombreRol(nombreRol);
        return role;
    }

    private StateEntity activeUserState() {
        StateEntity state = new StateEntity();
        state.setIdEstado(100L);
        state.setNombreEstado("ACTIVA");
        return state;
    }
}