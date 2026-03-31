package com.eap09.reservas.provideroffer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.AvailabilityOverlapException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceAvailabilityCreateRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceAvailabilityResponse;
import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import com.eap09.reservas.provideroffer.infrastructure.GeneralScheduleRepository;
import com.eap09.reservas.provideroffer.infrastructure.ServiceAvailabilityRepository;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import com.eap09.reservas.provideroffer.infrastructure.WeekDayRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ServiceAvailabilityServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceAvailabilityRepository serviceAvailabilityRepository;

    @Mock
    private GeneralScheduleRepository generalScheduleRepository;

    @Mock
    private WeekDayRepository weekDayRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private ServiceAvailabilityService serviceAvailabilityService;

    @Test
    void shouldCreateAvailabilitySuccessfully() {
        LocalDate mondayDate = LocalDate.of(2026, 4, 6);
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity service = serviceEntity(200L, 10L);
        WeekDayEntity monday = weekDay(1L, "LUNES");
        GeneralScheduleEntity schedule = scheduleEntity(10L, monday, LocalTime.of(8, 0), LocalTime.of(18, 0));
        StateEntity enabledState = state(30L, "HABILITADA");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L)).thenReturn(List.of(schedule));
        when(serviceAvailabilityRepository.existsOverlappingRange(200L, mondayDate, LocalTime.of(9, 0), LocalTime.of(10, 0))).thenReturn(false);
        when(stateRepository.findByCategoryAndStateName("tbl_disponibilidad_servicio", "HABILITADA")).thenReturn(Optional.of(enabledState));
        when(serviceAvailabilityRepository.save(any(ServiceAvailabilityEntity.class))).thenAnswer(invocation -> {
            ServiceAvailabilityEntity entity = invocation.getArgument(0);
            entity.setIdDisponibilidadServicio(500L);
            return entity;
        });

        ServiceAvailabilityResponse response = serviceAvailabilityService.createAvailability(
                "provider@test.local",
                200L,
                new ServiceAvailabilityCreateRequest(mondayDate, LocalTime.of(9, 0), LocalTime.of(10, 0))
        );

        assertEquals(500L, response.idDisponibilidad());
        assertEquals(200L, response.serviceId());
        assertEquals("HABILITADA", response.estadoDisponibilidad());
        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldRejectWhenServiceNotFound() {
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceAvailabilityService.createAvailability(
                "provider@test.local", 999L,
                new ServiceAvailabilityCreateRequest(LocalDate.of(2026, 4, 6), LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));
    }

    @Test
    void shouldRejectWhenServiceBelongsToAnotherProvider() {
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity foreignService = serviceEntity(200L, 99L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(foreignService));

        assertThrows(AccessDeniedException.class, () -> serviceAvailabilityService.createAvailability(
                "provider@test.local", 200L,
                new ServiceAvailabilityCreateRequest(LocalDate.of(2026, 4, 6), LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));
    }

    @Test
    void shouldRejectInvalidRange() {
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity service = serviceEntity(200L, 10L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));

        assertThrows(ApiException.class, () -> serviceAvailabilityService.createAvailability(
                "provider@test.local", 200L,
                new ServiceAvailabilityCreateRequest(LocalDate.of(2026, 4, 6), LocalTime.of(10, 0), LocalTime.of(10, 0))
        ));
    }

    @Test
    void shouldRejectOutsideGeneralSchedule() {
        LocalDate mondayDate = LocalDate.of(2026, 4, 6);
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity service = serviceEntity(200L, 10L);
        WeekDayEntity monday = weekDay(1L, "LUNES");
        GeneralScheduleEntity schedule = scheduleEntity(10L, monday, LocalTime.of(8, 0), LocalTime.of(12, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L)).thenReturn(List.of(schedule));

        assertThrows(ApiException.class, () -> serviceAvailabilityService.createAvailability(
                "provider@test.local", 200L,
                new ServiceAvailabilityCreateRequest(mondayDate, LocalTime.of(11, 0), LocalTime.of(13, 0))
        ));
    }

    @Test
    void shouldRejectOverlappingRange() {
        LocalDate mondayDate = LocalDate.of(2026, 4, 6);
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity service = serviceEntity(200L, 10L);
        WeekDayEntity monday = weekDay(1L, "LUNES");
        GeneralScheduleEntity schedule = scheduleEntity(10L, monday, LocalTime.of(8, 0), LocalTime.of(18, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L)).thenReturn(List.of(schedule));
        when(serviceAvailabilityRepository.existsOverlappingRange(200L, mondayDate, LocalTime.of(9, 0), LocalTime.of(10, 0))).thenReturn(true);

        assertThrows(AvailabilityOverlapException.class, () -> serviceAvailabilityService.createAvailability(
                "provider@test.local", 200L,
                new ServiceAvailabilityCreateRequest(mondayDate, LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));
    }

    @Test
    void shouldBlockAvailabilitySuccessfully() {
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity service = serviceEntity(200L, 10L);
        ServiceAvailabilityEntity availability = new ServiceAvailabilityEntity();
        availability.setIdDisponibilidadServicio(800L);
        availability.setIdServicio(200L);
        availability.setFechaDisponibilidad(LocalDate.of(2026, 4, 6));
        availability.setHoraInicio(LocalTime.of(9, 0));
        availability.setHoraFin(LocalTime.of(10, 0));
        availability.setFechaActualizacionDisponibilidad(OffsetDateTime.now());

        StateEntity blockedState = state(31L, "BLOQUEADA");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));
        when(serviceAvailabilityRepository.findById(800L)).thenReturn(Optional.of(availability));
        when(stateRepository.findByCategoryAndStateName("tbl_disponibilidad_servicio", "BLOQUEADA")).thenReturn(Optional.of(blockedState));
        when(serviceAvailabilityRepository.save(any(ServiceAvailabilityEntity.class))).thenAnswer(i -> i.getArgument(0));

        ServiceAvailabilityResponse response = serviceAvailabilityService.blockAvailability("provider@test.local", 200L, 800L);
        assertEquals("BLOQUEADA", response.estadoDisponibilidad());
        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldRejectBlockingWhenAvailabilityNotFound() {
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity service = serviceEntity(200L, 10L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));
        when(serviceAvailabilityRepository.findById(800L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> serviceAvailabilityService.blockAvailability("provider@test.local", 200L, 800L));
    }

    @Test
    void shouldRejectBlockingWhenAvailabilityBelongsToAnotherService() {
        UserAccountEntity provider = providerUser(10L, "provider@test.local");
        ServiceEntity service = serviceEntity(200L, 10L);
        ServiceAvailabilityEntity availability = new ServiceAvailabilityEntity();
        availability.setIdDisponibilidadServicio(800L);
        availability.setIdServicio(999L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local")).thenReturn(Optional.of(provider));
        when(serviceRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));
        when(serviceAvailabilityRepository.findById(800L)).thenReturn(Optional.of(availability));

        assertThrows(AccessDeniedException.class,
                () -> serviceAvailabilityService.blockAvailability("provider@test.local", 200L, 800L));
    }

    @Test
    void shouldRejectNonProviderUser() {
        UserAccountEntity client = providerUser(10L, "client@test.local");
        client.getRol().setNombreRol("CLIENTE");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local")).thenReturn(Optional.of(client));

        assertThrows(ProviderRoleRequiredException.class,
                () -> serviceAvailabilityService.createAvailability(
                        "client@test.local", 200L,
                        new ServiceAvailabilityCreateRequest(LocalDate.of(2026, 4, 6), LocalTime.of(9, 0), LocalTime.of(10, 0))
                ));

        verify(serviceAvailabilityRepository, never()).save(any(ServiceAvailabilityEntity.class));
    }

    private UserAccountEntity providerUser(Long id, String email) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol("PROVEEDOR");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(id);
        user.setCorreoUsuario(email);
        user.setRol(role);
        return user;
    }

    private ServiceEntity serviceEntity(Long serviceId, Long providerId) {
        ServiceEntity service = new ServiceEntity();
        service.setIdServicio(serviceId);
        service.setIdUsuarioProveedor(providerId);
        return service;
    }

    private WeekDayEntity weekDay(Long id, String name) {
        WeekDayEntity day = new WeekDayEntity();
        day.setIdDiaSemana(id);
        day.setNombreDiaSemana(name);
        return day;
    }

    private GeneralScheduleEntity scheduleEntity(Long providerId, WeekDayEntity day, LocalTime start, LocalTime end) {
        GeneralScheduleEntity entity = new GeneralScheduleEntity();
        entity.setIdUsuarioProveedor(providerId);
        entity.setDiaSemana(day);
        entity.setHoraInicio(start);
        entity.setHoraFin(end);
        return entity;
    }

    private StateEntity state(Long id, String name) {
        StateEntity state = new StateEntity();
        state.setIdEstado(id);
        state.setNombreEstado(name);
        return state;
    }
}