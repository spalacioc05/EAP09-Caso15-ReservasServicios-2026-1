package com.eap09.reservas.unittests.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ServiceNameAlreadyExistsException;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationRequest;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationResponse;
import com.eap09.reservas.provideroffer.application.ServiceRegistrationService;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;

public class ServiceRegistrationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private ServiceRegistrationService serviceRegistrationService;

    private StateEntity userActiveState;
    private UserAccountEntity user;
    private RoleEntity role;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        role = new RoleEntity();
        role.setIdRol(21L);
        role.setNombreRol("PROVEEDOR");

        userActiveState = new StateEntity();
        userActiveState.setIdEstado(1L);
        userActiveState.setNombreEstado("ACTIVA");

        user = new UserAccountEntity();
        user.setIdUsuario(10L);
        user.setCorreoUsuario("juan.medina@empresa.com");
        user.setRol(role);
        user.setIdEstado(userActiveState.getIdEstado());
    }

    @Test
    void RegisterService_Success() {
        ServiceEntity savedService = new ServiceEntity();
        savedService.setIdServicio(10L);
        savedService.setNombreServicio("Masaje terapeutico");
        savedService.setDescripcionServicio("Sesion de relajacion");
        savedService.setCapacidadMaximaConcurrente(2);
        savedService.setDuracionMinutos(60);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(user));
        when(serviceRepository.existsByProviderAndServiceName(10L, savedService.getNombreServicio()))
                .thenReturn(false);
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(userActiveState));
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(savedService);

        ServiceRegistrationResponse response = serviceRegistrationService.registerService(
                user.getCorreoUsuario(),
                new ServiceRegistrationRequest(savedService.getNombreServicio(), savedService.getDescripcionServicio(),
                        savedService.getDuracionMinutos(), savedService.getCapacidadMaximaConcurrente()));

        assertEquals(10L, response.idServicio());
        assertEquals("Masaje terapeutico", response.nombre());
        assertEquals(60, response.duracionMinutos());
        assertEquals(2, response.capacidadMaximaConcurrente());
    }

    @Test
    void EmptyName_Exception() {
        ServiceRegistrationRequest request = new ServiceRegistrationRequest("", "Sesion de relajacion", 60, 2);

        assertThrows(UnsupportedOperationException.class,
                () -> serviceRegistrationService.registerService("juan.medina@empresa.com", request));
    }

    @Test
    void RejectDuplicateServiceNameForSameProvider_Exception() {

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(user));
        when(serviceRepository.existsByProviderAndServiceName(10L, "Masaje terapeutico"))
                .thenReturn(true);

        assertThrows(ServiceNameAlreadyExistsException.class,
                () -> serviceRegistrationService.registerService(
                        "juan.medina@empresa.com",
                        new ServiceRegistrationRequest("Masaje terapeutico", "Sesion de relajacion", 60, 2)));
    }
}
