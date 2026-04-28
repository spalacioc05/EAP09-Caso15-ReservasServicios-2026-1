package com.eap09.reservas.provideroffer.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusResponse;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests para ServiceStatusService.
 * Pruebas enfocadas en la lógica de cambio de estado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceStatusService Tests")
class ServiceStatusServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    private ServiceStatusService service;

    @BeforeEach
    void setUp() {
        service = new ServiceStatusService(
                userAccountRepository,
                serviceRepository,
                stateRepository,
                systemEventPublisher);
    }

    // ========== Activación Exitosa ==========

    @Test
    @DisplayName("Activación exitosa: cambia estado INACTIVO a ACTIVO")
    void testActivateServiceSuccess() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;
        Long inactiveStateId = 2L;
        Long activeStateId = 1L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        StateEntity inactiveState = new StateEntity();
        inactiveState.setIdEstado(inactiveStateId);
        inactiveState.setNombreEstado("INACTIVO");

        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(activeStateId);
        activeState.setNombreEstado("ACTIVO");

        ServiceEntity service_entity = new ServiceEntity();
        service_entity.setIdServicio(serviceId);
        service_entity.setIdUsuarioProveedor(providerId);
        service_entity.setIdEstadoServicio(inactiveStateId);
        service_entity.setNombreServicio("Consultoría");
        service_entity.setFechaActualizacionServicio(OffsetDateTime.now());

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(serviceId))
                .thenReturn(Optional.of(service_entity));
        when(stateRepository.findById(inactiveStateId))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.save(any(ServiceEntity.class)))
                .thenAnswer(inv -> {
                    ServiceEntity saved = inv.getArgument(0);
                    saved.setIdServicio(serviceId);
                    return saved;
                });

        // Act
        ServiceStatusResponse response = service.updateServiceStatus(
                email,
                serviceId,
                new ServiceStatusRequest("ACTIVO"));

        // Assert
        assertNotNull(response);
        assertEquals(serviceId, response.idServicio());
        assertEquals("Consultoría", response.nombreServicio());
        assertEquals("ACTIVO", response.estadoActual());

        verify(serviceRepository).save(argThat(s -> s.getIdEstadoServicio().equals(activeStateId)));
        verify(systemEventPublisher).publish(argThat(event -> {
            assert event.type().equals("ACTIVACION_SERVICIO");
            assert event.result().equals("EXITO");
            return true;
        }));
    }

    // ========== Inactivación Exitosa ==========

    @Test
    @DisplayName("Inactivación exitosa: cambia estado ACTIVO a INACTIVO")
    void testDeactivateServiceSuccess() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;
        Long activeStateId = 1L;
        Long inactiveStateId = 2L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(activeStateId);
        activeState.setNombreEstado("ACTIVO");

        StateEntity inactiveState = new StateEntity();
        inactiveState.setIdEstado(inactiveStateId);
        inactiveState.setNombreEstado("INACTIVO");

        ServiceEntity service_entity = new ServiceEntity();
        service_entity.setIdServicio(serviceId);
        service_entity.setIdUsuarioProveedor(providerId);
        service_entity.setIdEstadoServicio(activeStateId);
        service_entity.setNombreServicio("Consultoría");
        service_entity.setFechaActualizacionServicio(OffsetDateTime.now());

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO"))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.findByIdServicio(serviceId))
                .thenReturn(Optional.of(service_entity));
        when(stateRepository.findById(activeStateId))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.save(any(ServiceEntity.class)))
                .thenAnswer(inv -> {
                    ServiceEntity saved = inv.getArgument(0);
                    saved.setIdServicio(serviceId);
                    return saved;
                });

        // Act
        ServiceStatusResponse response = service.updateServiceStatus(
                email,
                serviceId,
                new ServiceStatusRequest("INACTIVO"));

        // Assert
        assertNotNull(response);
        assertEquals(serviceId, response.idServicio());
        assertEquals("Consultoría", response.nombreServicio());
        assertEquals("INACTIVO", response.estadoActual());

        verify(serviceRepository).save(argThat(s -> s.getIdEstadoServicio().equals(inactiveStateId)));
        verify(systemEventPublisher).publish(argThat(event -> {
            assert event.type().equals("INACTIVACION_SERVICIO");
            assert event.result().equals("EXITO");
            return true;
        }));
    }

    // ========== Ya Estaba Activo ==========

    @Test
    @DisplayName("Ya estaba activo: idempotente, devuelve 200 OK sin cambios")
    void testAlreadyActiveShouldReturnIdempotent() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;
        Long activeStateId = 1L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(activeStateId);
        activeState.setNombreEstado("ACTIVO");

        ServiceEntity service_entity = new ServiceEntity();
        service_entity.setIdServicio(serviceId);
        service_entity.setIdUsuarioProveedor(providerId);
        service_entity.setIdEstadoServicio(activeStateId);
        service_entity.setNombreServicio("Consultoría");
        service_entity.setFechaActualizacionServicio(OffsetDateTime.now());

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(serviceId))
                .thenReturn(Optional.of(service_entity));
        when(stateRepository.findById(activeStateId))
                .thenReturn(Optional.of(activeState));

        // Act
        ServiceStatusResponse response = service.updateServiceStatus(
                email,
                serviceId,
                new ServiceStatusRequest("ACTIVO"));

        // Assert
        assertNotNull(response);
        assertEquals(serviceId, response.idServicio());
        assertEquals("ACTIVO", response.estadoActual());

        // No debe guardar si ya está activo
        verify(serviceRepository, never()).save(any());
        // No debe publicar evento si no hay cambio
        verify(systemEventPublisher, never()).publish(any());
    }

    // ========== Ya Estaba Inactivo ==========

    @Test
    @DisplayName("Ya estaba inactivo: idempotente, devuelve 200 OK sin cambios")
    void testAlreadyInactiveShouldReturnIdempotent() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;
        Long inactiveStateId = 2L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        StateEntity inactiveState = new StateEntity();
        inactiveState.setIdEstado(inactiveStateId);
        inactiveState.setNombreEstado("INACTIVO");

        ServiceEntity service_entity = new ServiceEntity();
        service_entity.setIdServicio(serviceId);
        service_entity.setIdUsuarioProveedor(providerId);
        service_entity.setIdEstadoServicio(inactiveStateId);
        service_entity.setNombreServicio("Consultoría");
        service_entity.setFechaActualizacionServicio(OffsetDateTime.now());

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO"))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.findByIdServicio(serviceId))
                .thenReturn(Optional.of(service_entity));
        when(stateRepository.findById(inactiveStateId))
                .thenReturn(Optional.of(inactiveState));

        // Act
        ServiceStatusResponse response = service.updateServiceStatus(
                email,
                serviceId,
                new ServiceStatusRequest("INACTIVO"));

        // Assert
        assertNotNull(response);
        assertEquals(serviceId, response.idServicio());
        assertEquals("INACTIVO", response.estadoActual());

        verify(serviceRepository, never()).save(any());
        verify(systemEventPublisher, never()).publish(any());
    }

    // ========== Servicio No Encontrado ==========

    @Test
    @DisplayName("Servicio no encontrado: devuelve 404 ResourceNotFoundException")
    void testServiceNotFoundThrows404() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 999L;
        Long providerId = 100L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName(any(), any()))
                .thenReturn(Optional.of(new StateEntity()));
        when(serviceRepository.findByIdServicio(serviceId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                service.updateServiceStatus(
                        email,
                        serviceId,
                        new ServiceStatusRequest("ACTIVO")));
    }

    // ========== Servicio Ajeno ==========

    @Test
    @DisplayName("Servicio ajeno: devuelve 403 AccessDeniedException")
    void testServiceNotOwnedByProviderThrows403() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;
        Long otherProviderId = 999L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(1L);
        activeState.setNombreEstado("ACTIVO");

        ServiceEntity service_entity = new ServiceEntity();
        service_entity.setIdServicio(serviceId);
        service_entity.setIdUsuarioProveedor(otherProviderId); // Diferente proveedor
        service_entity.setNombreServicio("Consultoría");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(serviceId))
                .thenReturn(Optional.of(service_entity));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                service.updateServiceStatus(
                        email,
                        serviceId,
                        new ServiceStatusRequest("ACTIVO")));
    }

    // ========== Usuario No Autenticado ==========

    @Test
    @DisplayName("Usuario no encontrado: devuelve 403 ProviderRoleRequiredException")
    void testUserNotFoundThrows403() {
        // Arrange
        String email = "nonexistent@mail.com";
        Long serviceId = 1L;

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProviderRoleRequiredException.class, () ->
                service.updateServiceStatus(
                        email,
                        serviceId,
                        new ServiceStatusRequest("ACTIVO")));
    }

    // ========== Usuario Sin Rol Proveedor ==========

    @Test
    @DisplayName("Usuario sin rol PROVEEDOR: devuelve 403 ProviderRoleRequiredException")
    void testNotProviderRoleThrows403() {
        // Arrange
        String email = "cliente@mail.com";
        Long serviceId = 1L;

        RoleEntity clientRole = new RoleEntity();
        clientRole.setNombreRol("CLIENTE");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(100L);
        user.setCorreoUsuario(email);
        user.setRol(clientRole);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(ProviderRoleRequiredException.class, () ->
                service.updateServiceStatus(
                        email,
                        serviceId,
                        new ServiceStatusRequest("ACTIVO")));
    }

    // ========== Estado Inválido ==========

    @Test
    @DisplayName("Estado inválido (ELIMINADO): devuelve 400 ApiException")
    void testInvalidStatusThrows400() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));

        // Act & Assert
        assertThrows(ApiException.class, () ->
                service.updateServiceStatus(
                        email,
                        serviceId,
                        new ServiceStatusRequest("ELIMINADO")));
    }

    // ========== Estado Nulo ==========

    @Test
    @DisplayName("Estado nulo: devuelve 400 ApiException")
    void testNullStatusThrows400() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));

        // Act & Assert
        assertThrows(ApiException.class, () ->
                service.updateServiceStatus(
                        email,
                        serviceId,
                        new ServiceStatusRequest(null)));
    }

    // ========== Validación de Fecha ==========

    @Test
    @DisplayName("Cambio exitoso actualiza fecha_actualizacion_servicio")
    void testUpdateChangesModificationDate() {
        // Arrange
        String email = "proveedor@mail.com";
        Long serviceId = 1L;
        Long providerId = 100L;
        Long inactiveStateId = 2L;
        Long activeStateId = 1L;

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        UserAccountEntity provider = new UserAccountEntity();
        provider.setIdUsuario(providerId);
        provider.setCorreoUsuario(email);
        provider.setRol(providerRole);

        StateEntity inactiveState = new StateEntity();
        inactiveState.setIdEstado(inactiveStateId);
        inactiveState.setNombreEstado("INACTIVO");

        StateEntity activeState = new StateEntity();
        activeState.setIdEstado(activeStateId);
        activeState.setNombreEstado("ACTIVO");

        OffsetDateTime oldDate = OffsetDateTime.now().minusHours(1);
        ServiceEntity service_entity = new ServiceEntity();
        service_entity.setIdServicio(serviceId);
        service_entity.setIdUsuarioProveedor(providerId);
        service_entity.setIdEstadoServicio(inactiveStateId);
        service_entity.setNombreServicio("Consultoría");
        service_entity.setFechaActualizacionServicio(oldDate);

        ArgumentCaptor<ServiceEntity> captor = ArgumentCaptor.forClass(ServiceEntity.class);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(serviceId))
                .thenReturn(Optional.of(service_entity));
        when(stateRepository.findById(inactiveStateId))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.save(any(ServiceEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.updateServiceStatus(
                email,
                serviceId,
                new ServiceStatusRequest("ACTIVO"));

        // Assert
        verify(serviceRepository).save(captor.capture());
        ServiceEntity saved = captor.getValue();
        assertTrue(saved.getFechaActualizacionServicio().isAfter(oldDate),
                "La fecha de actualización debe ser más reciente");
    }
}
