package com.eap09.reservas.unittests.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.time.OffsetDateTime;
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
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleResponse;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleUpsertRequest;
import com.eap09.reservas.provideroffer.application.GeneralScheduleService;
import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import com.eap09.reservas.provideroffer.infrastructure.GeneralScheduleRepository;
import com.eap09.reservas.provideroffer.infrastructure.WeekDayRepository;

public class GeneralScheduleServiceTest {
    @Mock
    private GeneralScheduleRepository generalScheduleRepository;

    @Mock
    private WeekDayRepository weekDayRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private GeneralScheduleService generalScheduleService;

    private UserAccountEntity user;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        role = new RoleEntity();
        role.setIdRol(2L);
        role.setNombreRol("PROVEEDOR");

        user = new UserAccountEntity();
        user.setIdUsuario(10L);
        user.setCorreoUsuario("juan.medina@empresa.com");
        user.setRol(role);

    }

    @Test
    void CreateGeneralSchedule_Success() {
        WeekDayEntity monday = new WeekDayEntity();
        monday.setIdDiaSemana(1L);
        monday.setNombreDiaSemana("LUNES");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(user));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L))
                .thenReturn(List.of());
        when(generalScheduleRepository.save(any())).thenReturn(monday);

        GeneralScheduleResponse response = generalScheduleService.upsertGeneralSchedule(
                user.getCorreoUsuario(),
                monday.getNombreDiaSemana(),
                new GeneralScheduleUpsertRequest(LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals(user.getIdUsuario(), response.providerUserId());
        assertEquals(monday.getNombreDiaSemana(), response.dayOfWeek());
        assertEquals(LocalTime.of(8, 0), response.horaInicio());
        assertEquals(LocalTime.of(12, 0), response.horaFin());

    }

    @Test
    void updateSchedule_Success() {
        WeekDayEntity friday = new WeekDayEntity();
        friday.setIdDiaSemana(6L);
        friday.setNombreDiaSemana("VIERNES");

        GeneralScheduleEntity lastSchedule = new GeneralScheduleEntity();
        lastSchedule.setIdHorarioGeneralProveedor(11L);
        lastSchedule.setIdUsuarioProveedor(10L);
        lastSchedule.setDiaSemana(friday);
        lastSchedule.setFechaCreacionHorarioGeneral(OffsetDateTime.now().minusDays(5));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(user));
        when(weekDayRepository.findByNombreDiaSemana("VIERNES")).thenReturn(Optional.of(friday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 6L))
                .thenReturn(List.of(lastSchedule));
        when(generalScheduleRepository.save(any())).thenReturn(lastSchedule);

        GeneralScheduleResponse response = generalScheduleService.upsertGeneralSchedule(
                user.getCorreoUsuario(),
                friday.getNombreDiaSemana(),
                new GeneralScheduleUpsertRequest(LocalTime.of(9, 0), LocalTime.of(13, 0)));

        assertEquals(user.getIdUsuario(), response.providerUserId());
        assertEquals(friday.getNombreDiaSemana(), response.dayOfWeek());
        assertEquals(LocalTime.of(9, 0), response.horaInicio());
        assertEquals(LocalTime.of(13, 0), response.horaFin());
    }

    @Test
    void IncorrectSchedule_Exception() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(user));

        assertThrows(ApiException.class, () -> generalScheduleService.upsertGeneralSchedule(
                "juan.medina@empresa.com",
                "LUNES",
                new GeneralScheduleUpsertRequest(LocalTime.of(14, 0), LocalTime.of(10, 0))));

    }

}
