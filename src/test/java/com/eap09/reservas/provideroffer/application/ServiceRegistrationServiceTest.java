package com.eap09.reservas.provideroffer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ServiceNameAlreadyExistsException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationResponse;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceRegistrationServiceTest {

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

    @Test
    void shouldRegisterServiceSuccessfully() {
        UserAccountEntity provider = providerUser();

        StateEntity activeServiceState = new StateEntity();
        activeServiceState.setIdEstado(21L);
        activeServiceState.setNombreEstado("ACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(serviceRepository.existsByProviderAndServiceName(10L, "Masaje terapeutico"))
                .thenReturn(false);
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeServiceState));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> {
            ServiceEntity entity = invocation.getArgument(0);
            entity.setIdServicio(500L);
            return entity;
        });

        ServiceRegistrationResponse response = serviceRegistrationService.registerService(
                "provider@test.local",
                new ServiceRegistrationRequest("Masaje terapeutico", "Sesion de relajacion", 60, 2)
        );

        assertEquals(500L, response.idServicio());
        assertEquals("Masaje terapeutico", response.nombre());
        assertEquals("Sesion de relajacion", response.descripcion());
        assertEquals(60, response.duracionMinutos());
        assertEquals(2, response.capacidadMaximaConcurrente());
        assertEquals("ACTIVO", response.estadoServicio());

        ArgumentCaptor<ServiceEntity> captor = ArgumentCaptor.forClass(ServiceEntity.class);
        verify(serviceRepository).save(captor.capture());
        ServiceEntity saved = captor.getValue();
        assertEquals(10L, saved.getIdUsuarioProveedor());
        assertEquals(21L, saved.getIdEstadoServicio());
        assertEquals("Masaje terapeutico", saved.getNombreServicio());

        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldRejectDuplicateServiceNameForSameProvider() {
        UserAccountEntity provider = providerUser();

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(serviceRepository.existsByProviderAndServiceName(10L, "Masaje terapeutico"))
                .thenReturn(true);

        assertThrows(ServiceNameAlreadyExistsException.class,
                () -> serviceRegistrationService.registerService(
                        "provider@test.local",
                        new ServiceRegistrationRequest("Masaje terapeutico", "Sesion de relajacion", 60, 2)
                ));

        verify(serviceRepository, never()).save(any(ServiceEntity.class));
        verify(systemEventPublisher, never()).publish(any());
    }

    @Test
    void shouldAllowSameNameForDifferentProvider() {
        UserAccountEntity provider = providerUser();
        provider.setIdUsuario(11L);
        provider.setCorreoUsuario("provider2@test.local");

        StateEntity activeServiceState = new StateEntity();
        activeServiceState.setIdEstado(21L);
        activeServiceState.setNombreEstado("ACTIVO");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider2@test.local"))
                .thenReturn(Optional.of(provider));
        when(serviceRepository.existsByProviderAndServiceName(11L, "Masaje terapeutico"))
                .thenReturn(false);
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeServiceState));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> {
            ServiceEntity entity = invocation.getArgument(0);
            entity.setIdServicio(600L);
            return entity;
        });

        ServiceRegistrationResponse response = serviceRegistrationService.registerService(
                "provider2@test.local",
                new ServiceRegistrationRequest("Masaje terapeutico", "Mismo nombre otro proveedor", 45, 1)
        );

        assertEquals(600L, response.idServicio());
        assertEquals("Masaje terapeutico", response.nombre());
    }

    @Test
    void shouldRejectInvalidDuration() {
        UserAccountEntity provider = providerUser();

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));

        assertThrows(ApiException.class,
                () -> serviceRegistrationService.registerService(
                        "provider@test.local",
                        new ServiceRegistrationRequest("Masaje terapeutico", "Sesion de relajacion", 0, 2)
                ));
    }

    @Test
    void shouldRejectInvalidCapacity() {
        UserAccountEntity provider = providerUser();

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));

        assertThrows(ApiException.class,
                () -> serviceRegistrationService.registerService(
                        "provider@test.local",
                        new ServiceRegistrationRequest("Masaje terapeutico", "Sesion de relajacion", 60, 0)
                ));
    }

    @Test
    void shouldRejectNonProviderUser() {
        UserAccountEntity client = providerUser();
        client.getRol().setNombreRol("CLIENTE");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(client));

        assertThrows(ProviderRoleRequiredException.class,
                () -> serviceRegistrationService.registerService(
                        "provider@test.local",
                        new ServiceRegistrationRequest("Masaje terapeutico", "Sesion de relajacion", 60, 2)
                ));
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
}