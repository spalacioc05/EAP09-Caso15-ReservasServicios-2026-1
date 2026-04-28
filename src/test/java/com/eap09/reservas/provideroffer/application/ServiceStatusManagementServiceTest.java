package com.eap09.reservas.provideroffer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.ServiceStatusAlreadySetException;
import com.eap09.reservas.common.exception.ServiceStatusChangeFailedException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateResponse;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ServiceStatusManagementServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private ServiceStatusManagementService serviceStatusManagementService;

    @Test
    void shouldActivateInactiveOwnServiceSuccessfully() {
        UserAccountEntity provider = providerUser();
        ServiceEntity service = ownService(200L, 10L, 2L, "Servicio Inactivo");
        StateEntity activeState = serviceState(1L, "ACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(200L))
                .thenReturn(Optional.of(service));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceStatusUpdateResponse response = serviceStatusManagementService.updateOwnServiceStatus(
                "provider@test.local",
                200L,
                new ServiceStatusUpdateRequest("ACTIVO"));

        assertEquals(200L, response.idServicio());
        assertEquals("Servicio Inactivo", response.nombre());
        assertEquals("ACTIVO", response.estadoServicio());
        assertEquals(1L, service.getIdEstadoServicio());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("ACTIVACION_SERVICIO", eventCaptor.getValue().type());
        assertEquals("EXITO", eventCaptor.getValue().result());
    }

    @Test
    void shouldInactivateActiveOwnServiceSuccessfully() {
        UserAccountEntity provider = providerUser();
        ServiceEntity service = ownService(201L, 10L, 1L, "Servicio Activo");
        StateEntity inactiveState = serviceState(2L, "INACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO"))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.findByIdServicio(201L))
                .thenReturn(Optional.of(service));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceStatusUpdateResponse response = serviceStatusManagementService.updateOwnServiceStatus(
                "provider@test.local",
                201L,
                new ServiceStatusUpdateRequest("INACTIVO"));

        assertEquals("INACTIVO", response.estadoServicio());
        assertEquals(2L, service.getIdEstadoServicio());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("INACTIVACION_SERVICIO", eventCaptor.getValue().type());
        assertEquals("EXITO", eventCaptor.getValue().result());
    }

    @Test
    void shouldInactivateOwnServiceWithCreatedReservationsWithoutTouchingThem() {
        UserAccountEntity provider = providerUser();
        ServiceEntity service = ownService(202L, 10L, 1L, "Servicio con Reservas");
        StateEntity inactiveState = serviceState(2L, "INACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO"))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.findByIdServicio(202L))
                .thenReturn(Optional.of(service));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceStatusUpdateResponse response = serviceStatusManagementService.updateOwnServiceStatus(
                "provider@test.local",
                202L,
                new ServiceStatusUpdateRequest("INACTIVO"));

        assertEquals("INACTIVO", response.estadoServicio());
        verify(serviceRepository).save(service);
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectActivationWhenServiceAlreadyActive() {
        UserAccountEntity provider = providerUser();
        ServiceEntity service = ownService(203L, 10L, 1L, "Servicio Activo");
        StateEntity activeState = serviceState(1L, "ACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(203L))
                .thenReturn(Optional.of(service));

        ServiceStatusAlreadySetException exception = assertThrows(ServiceStatusAlreadySetException.class,
                () -> serviceStatusManagementService.updateOwnServiceStatus(
                        "provider@test.local",
                        203L,
                        new ServiceStatusUpdateRequest("ACTIVO")));

        assertEquals("El servicio ya se encuentra activo", exception.getMessage());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("ACTIVACION_SERVICIO", eventCaptor.getValue().type());
        assertEquals("FALLO", eventCaptor.getValue().result());
        verify(serviceRepository, never()).save(any(ServiceEntity.class));
    }

    @Test
    void shouldRejectInactivationWhenServiceAlreadyInactive() {
        UserAccountEntity provider = providerUser();
        ServiceEntity service = ownService(204L, 10L, 2L, "Servicio Inactivo");
        StateEntity inactiveState = serviceState(2L, "INACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO"))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.findByIdServicio(204L))
                .thenReturn(Optional.of(service));

        ServiceStatusAlreadySetException exception = assertThrows(ServiceStatusAlreadySetException.class,
                () -> serviceStatusManagementService.updateOwnServiceStatus(
                        "provider@test.local",
                        204L,
                        new ServiceStatusUpdateRequest("INACTIVO")));

        assertEquals("El servicio ya se encuentra inactivo", exception.getMessage());
        verify(serviceRepository, never()).save(any(ServiceEntity.class));
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenServiceBelongsToAnotherProvider() {
        UserAccountEntity provider = providerUser();
        ServiceEntity service = ownService(205L, 99L, 1L, "Servicio Ajeno");
        StateEntity inactiveState = serviceState(2L, "INACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO"))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.findByIdServicio(205L))
                .thenReturn(Optional.of(service));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> serviceStatusManagementService.updateOwnServiceStatus(
                        "provider@test.local",
                        205L,
                        new ServiceStatusUpdateRequest("INACTIVO")));

        assertEquals("No tiene permisos para cambiar el estado de este servicio", exception.getMessage());
        verify(serviceRepository, never()).save(any(ServiceEntity.class));

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("INACTIVACION_SERVICIO", eventCaptor.getValue().type());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @Test
    void shouldRejectWhenServiceDoesNotExist() {
        UserAccountEntity provider = providerUser();
        StateEntity activeState = serviceState(1L, "ACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> serviceStatusManagementService.updateOwnServiceStatus(
                        "provider@test.local",
                        999L,
                        new ServiceStatusUpdateRequest("ACTIVO")));

        verify(serviceRepository, never()).save(any(ServiceEntity.class));
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldTranslateInternalDataErrorIntoControlledFailure() {
        UserAccountEntity provider = providerUser();
        ServiceEntity service = ownService(206L, 10L, 2L, "Servicio Fallido");
        StateEntity activeState = serviceState(1L, "ACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(206L))
                .thenReturn(Optional.of(service));
        when(serviceRepository.save(any(ServiceEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));

        ServiceStatusChangeFailedException exception = assertThrows(ServiceStatusChangeFailedException.class,
                () -> serviceStatusManagementService.updateOwnServiceStatus(
                        "provider@test.local",
                        206L,
                        new ServiceStatusUpdateRequest("ACTIVO")));

        assertEquals("No fue posible completar el cambio de estado del servicio. Intenta nuevamente mas tarde", exception.getMessage());
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotProvider() {
        UserAccountEntity client = providerUser();
        client.getRol().setNombreRol("CLIENTE");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(client));

        assertThrows(ProviderRoleRequiredException.class,
                () -> serviceStatusManagementService.updateOwnServiceStatus(
                        "provider@test.local",
                        207L,
                        new ServiceStatusUpdateRequest("ACTIVO")));
    }

    private UserAccountEntity providerUser() {
        RoleEntity role = new RoleEntity();
        role.setNombreRol("PROVEEDOR");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(10L);
        user.setCorreoUsuario("provider@test.local");
        user.setRol(role);
        return user;
    }

    private StateEntity serviceState(Long idEstado, String nombreEstado) {
        StateEntity state = new StateEntity();
        state.setIdEstado(idEstado);
        state.setNombreEstado(nombreEstado);
        return state;
    }

    private ServiceEntity ownService(Long serviceId, Long providerId, Long stateId, String name) {
        ServiceEntity service = new ServiceEntity();
        service.setIdServicio(serviceId);
        service.setIdUsuarioProveedor(providerId);
        service.setIdEstadoServicio(stateId);
        service.setNombreServicio(name);
        service.setDescripcionServicio("Descripcion");
        service.setDuracionMinutos(60);
        service.setCapacidadMaximaConcurrente(2);
        service.setFechaCreacionServicio(OffsetDateTime.now().minusDays(1));
        service.setFechaActualizacionServicio(OffsetDateTime.now().minusHours(1));
        return service;
    }
}