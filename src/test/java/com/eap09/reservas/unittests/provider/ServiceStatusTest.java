package com.eap09.reservas.unittests.provider;

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

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ServiceStatusAlreadySetException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateResponse;
import com.eap09.reservas.provideroffer.application.ServiceStatusManagementService;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;

public class ServiceStatusTest {
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

    private StateEntity activeState;
    private UserAccountEntity provider;
    private RoleEntity role;
    private ServiceEntity service;
    private StateEntity inactiveState;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        role = new RoleEntity();
        role.setIdRol(21L);
        role.setNombreRol("PROVEEDOR");

        activeState = new StateEntity();
        activeState.setIdEstado(1L);
        activeState.setNombreEstado("ACTIVA");

        provider = new UserAccountEntity();
        provider.setIdUsuario(10L);
        provider.setCorreoUsuario("juan.medina@empresa.com");
        provider.setRol(role);
        provider.setIdEstado(activeState.getIdEstado());

        service = new ServiceEntity();
        service.setIdServicio(200L);
        service.setNombreServicio("Peluquería");
        service.setIdUsuarioProveedor(10L);
        service.setIdEstadoServicio(2L);

        inactiveState = new StateEntity();
        inactiveState.setIdEstado(2L);
        inactiveState.setNombreEstado("INACTIVA");
    }

    @Test
    void ActivateOwnService_Success() {

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeState));
        when(serviceRepository.findByIdServicio(200L))
                .thenReturn(Optional.of(service));
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(service);

        ServiceStatusUpdateResponse response = serviceStatusManagementService.updateOwnServiceStatus(
                "juan.medina@empresa.com",
                200L,
                new ServiceStatusUpdateRequest("ACTIVO"));

        assertEquals(200L, response.idServicio());
        assertEquals("Peluquería", response.nombre());
        assertEquals("ACTIVA", response.estadoServicio());
        assertEquals(1L, service.getIdEstadoServicio());
    }

    @Test
    void DeactivateOwnServiceWithActiveReservations_Exception() {

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(provider));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO"))
                .thenReturn(Optional.of(inactiveState));
        when(serviceRepository.findByIdServicio(200L))
                .thenReturn(Optional.of(service));

        assertThrows(ServiceStatusAlreadySetException.class,
                () -> serviceStatusManagementService.updateOwnServiceStatus(
                        "juan.medina@empresa.com",
                        200L,
                        new ServiceStatusUpdateRequest("INACTIVO")));
    }
}
