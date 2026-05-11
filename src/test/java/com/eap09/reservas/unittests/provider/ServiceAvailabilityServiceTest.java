package com.eap09.reservas.unittests.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceAvailabilityCreateRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceAvailabilityResponse;
import com.eap09.reservas.provideroffer.application.ServiceAvailabilityService;
import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import com.eap09.reservas.provideroffer.infrastructure.GeneralScheduleRepository;
import com.eap09.reservas.provideroffer.infrastructure.ServiceAvailabilityRepository;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import com.eap09.reservas.provideroffer.infrastructure.WeekDayRepository;

public class ServiceAvailabilityServiceTest {

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

    private UserAccountEntity user;
    private ServiceEntity service;
    private WeekDayEntity monday;
    private GeneralScheduleEntity schedule;
    private StateEntity enabledState;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        RoleEntity role = new RoleEntity();
        role.setNombreRol("PROVEEDOR");

        user = new UserAccountEntity();
        user.setIdUsuario(10L);
        user.setCorreoUsuario("juan.medina@empresa.com");
        user.setRol(role);

        service = new ServiceEntity();
        service.setIdServicio(200L);
        service.setIdUsuarioProveedor(10L);

        monday = new WeekDayEntity();
        monday.setIdDiaSemana(1L);
        monday.setNombreDiaSemana("LUNES");

        schedule = new GeneralScheduleEntity();
        schedule.setIdUsuarioProveedor(10L);
        schedule.setDiaSemana(monday);
        schedule.setHoraInicio(LocalTime.of(8, 0));
        schedule.setHoraFin(LocalTime.of(18, 0));

        enabledState = new StateEntity();
        enabledState.setIdEstado(1L);
        enabledState.setNombreEstado("HABILITADA");
    }

    @Test
    void RegisterAvailability_Success() {
        LocalDate date = LocalDate.of(2026, 5, 11);
        ServiceAvailabilityCreateRequest request = new ServiceAvailabilityCreateRequest(
                date, LocalTime.of(10, 0), LocalTime.of(12, 0));

        ServiceAvailabilityEntity savedEntity = new ServiceAvailabilityEntity();
        savedEntity.setIdDisponibilidadServicio(500L);
        savedEntity.setIdServicio(200L);
        savedEntity.setFechaDisponibilidad(date);
        savedEntity.setHoraInicio(LocalTime.of(10, 0));
        savedEntity.setHoraFin(LocalTime.of(12, 0));
        savedEntity.setIdEstadoDisponibilidad(enabledState.getIdEstado());

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(user.getCorreoUsuario()))
                .thenReturn(Optional.of(user));
        when(serviceRepository.findByIdServicio(service.getIdServicio())).thenReturn(Optional.of(service));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L))
                .thenReturn(List.of(schedule));
        when(serviceAvailabilityRepository.existsOverlappingRange(anyLong(), any(), any(), any())).thenReturn(false);
        when(stateRepository.findByCategoryAndStateName("tbl_disponibilidad_servicio", "HABILITADA"))
                .thenReturn(Optional.of(enabledState));
        when(serviceAvailabilityRepository.save(any())).thenReturn(savedEntity);

        ServiceAvailabilityResponse response = serviceAvailabilityService.createAvailability(
                user.getCorreoUsuario(), service.getIdServicio(), request);

        assertEquals(500L, response.idDisponibilidad());
        assertEquals(200L, response.serviceId());
        assertEquals("HABILITADA", response.estadoDisponibilidad());
    }

    @Test
    void AvailabilityOutsideSchedule_Exception() {
        LocalDate date = LocalDate.of(2026, 5, 11);
        ServiceAvailabilityCreateRequest request = new ServiceAvailabilityCreateRequest(
                date, LocalTime.of(7, 0), LocalTime.of(9, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(serviceRepository.findByIdServicio(anyLong())).thenReturn(Optional.of(service));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L))
                .thenReturn(List.of(schedule));

        assertThrows(ApiException.class, () -> serviceAvailabilityService.createAvailability(
                user.getCorreoUsuario(), service.getIdServicio(), request));
    }

    @Test
    void InvalidTimeRange_Exception() {
        LocalDate date = LocalDate.of(2026, 5, 11);
        ServiceAvailabilityCreateRequest request = new ServiceAvailabilityCreateRequest(
                date, LocalTime.of(12, 0), LocalTime.of(10, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(serviceRepository.findByIdServicio(anyLong())).thenReturn(Optional.of(service));

        assertThrows(ApiException.class, () -> serviceAvailabilityService.createAvailability(
                user.getCorreoUsuario(), service.getIdServicio(), request));
    }

    @Test
    void EmptyFields_Exception() {
        ServiceAvailabilityCreateRequest request = new ServiceAvailabilityCreateRequest(null, null, null);

        assertThrows(UnsupportedOperationException.class,
                () -> serviceAvailabilityService.createAvailability(
                        user.getCorreoUsuario(), service.getIdServicio(), request));
    }
}
