package com.eap09.reservas.administration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.administration.api.dto.UserAccountStatusUpdateRequest;
import com.eap09.reservas.administration.api.dto.UserAccountStatusUpdateResponse;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.UserAccountStatusAlreadySetException;
import com.eap09.reservas.common.exception.UserAccountStatusUpdateFailedException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateCategoryEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
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
class UserAccountStatusAdministrationServiceTest {

    private static final String ADMIN_EMAIL = "admin@reservas.test";
    private static final String CLIENT_EMAIL = "cliente@reservas.test";
    private static final Long ACTIVE_STATE_ID = 100L;
    private static final Long INACTIVE_STATE_ID = 200L;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private UserAccountStatusAdministrationService userAccountStatusAdministrationService;

    @Test
    void shouldActivateInactiveUserSuccessfully() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);
        UserAccountEntity targetUser = buildUser(20L, CLIENT_EMAIL, "CLIENTE", INACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        mockUserStates();
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccountStatusUpdateResponse response = userAccountStatusAdministrationService.updateUserAccountStatus(
                ADMIN_EMAIL,
                20L,
                new UserAccountStatusUpdateRequest("activa"));

        assertEquals(20L, response.idUsuario());
        assertEquals(CLIENT_EMAIL, response.correo());
        assertEquals("INACTIVA", response.estadoAnterior());
        assertEquals("ACTIVA", response.estadoActual());
        assertEquals(ACTIVE_STATE_ID, targetUser.getIdEstado());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("ACTUALIZACION_ESTADO_USUARIO", eventCaptor.getValue().type());
        assertEquals("EXITO", eventCaptor.getValue().result());
        assertEquals("Estado de cuenta actualizado de INACTIVA a ACTIVA", eventCaptor.getValue().details());
    }

    @Test
    void shouldDeactivateActiveUserSuccessfully() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);
        UserAccountEntity targetUser = buildUser(20L, CLIENT_EMAIL, "CLIENTE", ACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        mockUserStates();
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccountStatusUpdateResponse response = userAccountStatusAdministrationService.updateUserAccountStatus(
                ADMIN_EMAIL,
                20L,
                new UserAccountStatusUpdateRequest("INACTIVA"));

        assertEquals("ACTIVA", response.estadoAnterior());
        assertEquals("INACTIVA", response.estadoActual());
        assertEquals(INACTIVE_STATE_ID, targetUser.getIdEstado());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("EXITO", eventCaptor.getValue().result());
        assertEquals("Estado de cuenta actualizado de ACTIVA a INACTIVA", eventCaptor.getValue().details());
    }

    @Test
    void shouldRejectWhenActivatingAlreadyActiveUser() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);
        UserAccountEntity targetUser = buildUser(20L, CLIENT_EMAIL, "CLIENTE", ACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        mockUserStates();
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));

        UserAccountStatusAlreadySetException exception = assertThrows(
                UserAccountStatusAlreadySetException.class,
                () -> userAccountStatusAdministrationService.updateUserAccountStatus(
                        ADMIN_EMAIL,
                        20L,
                        new UserAccountStatusUpdateRequest("ACTIVA")));

        assertEquals("La cuenta ya se encontraba activada", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
        assertEquals("La cuenta ya se encontraba activada", eventCaptor.getValue().details());
    }

    @Test
    void shouldRejectWhenDeactivatingAlreadyInactiveUser() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);
        UserAccountEntity targetUser = buildUser(20L, CLIENT_EMAIL, "CLIENTE", INACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        mockUserStates();
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));

        UserAccountStatusAlreadySetException exception = assertThrows(
                UserAccountStatusAlreadySetException.class,
                () -> userAccountStatusAdministrationService.updateUserAccountStatus(
                        ADMIN_EMAIL,
                        20L,
                        new UserAccountStatusUpdateRequest("INACTIVA")));

        assertEquals("La cuenta ya se encontraba desactivada", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
        assertEquals("La cuenta ya se encontraba desactivada", eventCaptor.getValue().details());
    }

    @Test
    void shouldRejectWhenTargetUserDoesNotExist() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        mockUserStates();
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userAccountStatusAdministrationService.updateUserAccountStatus(
                        ADMIN_EMAIL,
                        20L,
                        new UserAccountStatusUpdateRequest("ACTIVA")));

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenRequestedStatusIsInvalid() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> userAccountStatusAdministrationService.updateUserAccountStatus(
                        ADMIN_EMAIL,
                        20L,
                        new UserAccountStatusUpdateRequest("BLOQUEADA")));

        assertEquals("INVALID_USER_ACCOUNT_STATUS", exception.getErrorCode());
        assertEquals("El estado solicitado no es valido para usuarios", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccountEntity.class));
        verify(systemEventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() {
        UserAccountEntity providerUser = buildUser(1L, "provider@reservas.test", "PROVEEDOR", ACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@reservas.test"))
                .thenReturn(Optional.of(providerUser));

        AdminRoleRequiredException exception = assertThrows(
                AdminRoleRequiredException.class,
                () -> userAccountStatusAdministrationService.updateUserAccountStatus(
                        "provider@reservas.test",
                        20L,
                        new UserAccountStatusUpdateRequest("INACTIVA")));

        assertEquals(
                "Solo un administrador autenticado puede actualizar el estado de cuentas de usuario",
                exception.getMessage());
        verify(stateRepository, never()).findByCategoryAndStateName(any(), any());
        verify(systemEventPublisher, never()).publish(any());
    }

    @Test
    void shouldPreserveOtherUserDataWhenUpdatingStatus() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);
        UserAccountEntity targetUser = buildUser(20L, CLIENT_EMAIL, "CLIENTE", ACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        mockUserStates();
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userAccountStatusAdministrationService.updateUserAccountStatus(
                ADMIN_EMAIL,
                20L,
                new UserAccountStatusUpdateRequest("INACTIVA"));

        assertEquals("CLIENTE", targetUser.getRol().getNombreRol());
        assertEquals("Ana", targetUser.getNombresUsuario());
        assertEquals("Cliente", targetUser.getApellidosUsuario());
        assertEquals(CLIENT_EMAIL, targetUser.getCorreoUsuario());
        assertEquals("hash-seguro", targetUser.getHashContrasenaUsuario());
    }

    @Test
    void shouldRejectWhenAuthenticationIsMissing() {
        UserAccountStatusUpdateRequest request = new UserAccountStatusUpdateRequest("ACTIVA");

        InsufficientAuthenticationException exception = assertThrows(
                InsufficientAuthenticationException.class,
                () -> userAccountStatusAdministrationService.updateUserAccountStatus(" ", 20L, request));

        assertEquals("Autenticacion requerida", exception.getMessage());
        verify(userAccountRepository, never()).findByCorreoUsuarioIgnoreCase(any());
        verify(systemEventPublisher, never()).publish(any());
    }

    @Test
    void shouldTranslateUnexpectedDataAccessFailureIntoControlledException() {
        UserAccountEntity adminUser = buildUser(1L, ADMIN_EMAIL, "ADMINISTRADOR", ACTIVE_STATE_ID);
        UserAccountEntity targetUser = buildUser(20L, CLIENT_EMAIL, "CLIENTE", ACTIVE_STATE_ID);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        mockUserStates();
        when(userAccountRepository.findById(20L))
                .thenReturn(Optional.of(targetUser));
        when(userAccountRepository.save(any(UserAccountEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));

        UserAccountStatusUpdateFailedException exception = assertThrows(
                UserAccountStatusUpdateFailedException.class,
                () -> userAccountStatusAdministrationService.updateUserAccountStatus(
                        ADMIN_EMAIL,
                        20L,
                        new UserAccountStatusUpdateRequest("INACTIVA")));

        assertEquals(
                "No fue posible completar la actualizacion del estado de la cuenta de usuario. Intenta nuevamente mas tarde",
                exception.getMessage());
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    private void mockUserStates() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(buildState(ACTIVE_STATE_ID, "ACTIVA")));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "INACTIVA"))
                .thenReturn(Optional.of(buildState(INACTIVE_STATE_ID, "INACTIVA")));
    }

    private UserAccountEntity buildUser(Long idUsuario, String correo, String nombreRol, Long idEstado) {
        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(idUsuario);
        user.setCorreoUsuario(correo);
        user.setRol(buildRole(1L, nombreRol));
        user.setIdEstado(idEstado);
        user.setNombresUsuario("Ana");
        user.setApellidosUsuario("Cliente");
        user.setHashContrasenaUsuario("hash-seguro");
        user.setIntentosFallidosConsecutivos(0);
        return user;
    }

    private RoleEntity buildRole(Long idRol, String nombreRol) {
        RoleEntity role = new RoleEntity();
        role.setIdRol(idRol);
        role.setNombreRol(nombreRol);
        return role;
    }

    private StateEntity buildState(Long idEstado, String nombreEstado) {
        StateCategoryEntity category = new StateCategoryEntity();
        category.setIdCategoriaEstado(1L);
        category.setNombreCategoriaEstado("tbl_usuario");

        StateEntity state = new StateEntity();
        state.setIdEstado(idEstado);
        state.setNombreEstado(nombreEstado);
        state.setCategoriaEstado(category);
        return state;
    }
}