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
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleResponse;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleUpsertRequest;
import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import com.eap09.reservas.provideroffer.infrastructure.GeneralScheduleRepository;
import com.eap09.reservas.provideroffer.infrastructure.WeekDayRepository;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneralScheduleServiceTest {

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

    @Test
    void shouldCreateGeneralScheduleSuccessfully() {
        UserAccountEntity provider = providerUser();
        WeekDayEntity monday = monday();

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L))
                .thenReturn(List.of());
        when(generalScheduleRepository.save(any(GeneralScheduleEntity.class))).thenAnswer(invocation -> {
            GeneralScheduleEntity entity = invocation.getArgument(0);
            entity.setIdHorarioGeneralProveedor(100L);
            return entity;
        });

        GeneralScheduleResponse response = generalScheduleService.upsertGeneralSchedule(
                "provider@test.local",
                "LUNES",
                new GeneralScheduleUpsertRequest(LocalTime.of(8, 0), LocalTime.of(12, 0))
        );

        assertEquals(10L, response.providerUserId());
        assertEquals("LUNES", response.dayOfWeek());
        assertEquals(LocalTime.of(8, 0), response.horaInicio());
        assertEquals(LocalTime.of(12, 0), response.horaFin());

        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldReplaceExistingScheduleAndKeepSingleRange() {
        UserAccountEntity provider = providerUser();
        WeekDayEntity monday = monday();

        GeneralScheduleEntity existing1 = new GeneralScheduleEntity();
        existing1.setIdHorarioGeneralProveedor(11L);
        existing1.setIdUsuarioProveedor(10L);
        existing1.setDiaSemana(monday);
        existing1.setFechaCreacionHorarioGeneral(OffsetDateTime.now().minusDays(1));

        GeneralScheduleEntity existing2 = new GeneralScheduleEntity();
        existing2.setIdHorarioGeneralProveedor(12L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(weekDayRepository.findByNombreDiaSemana("LUNES")).thenReturn(Optional.of(monday));
        when(generalScheduleRepository.findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(10L, 1L))
                .thenReturn(List.of(existing1, existing2));
        when(generalScheduleRepository.save(any(GeneralScheduleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        generalScheduleService.upsertGeneralSchedule(
                "provider@test.local",
                "LUNES",
                new GeneralScheduleUpsertRequest(LocalTime.of(9, 0), LocalTime.of(13, 0))
        );

        verify(generalScheduleRepository).deleteAll(List.of(existing2));

        ArgumentCaptor<GeneralScheduleEntity> captor = ArgumentCaptor.forClass(GeneralScheduleEntity.class);
        verify(generalScheduleRepository).save(captor.capture());
        GeneralScheduleEntity saved = captor.getValue();
        assertEquals(11L, saved.getIdHorarioGeneralProveedor());
        assertEquals(LocalTime.of(9, 0), saved.getHoraInicio());
        assertEquals(LocalTime.of(13, 0), saved.getHoraFin());
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        UserAccountEntity provider = providerUser();

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));

        assertThrows(ApiException.class, () -> generalScheduleService.upsertGeneralSchedule(
                "provider@test.local",
                "LUNES",
                new GeneralScheduleUpsertRequest(LocalTime.of(14, 0), LocalTime.of(10, 0))
        ));

        verify(generalScheduleRepository, never()).save(any());
    }

    @Test
    void shouldRejectNonProviderUser() {
        UserAccountEntity client = providerUser();
        client.getRol().setNombreRol("CLIENTE");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(client));

        assertThrows(ProviderRoleRequiredException.class, () -> generalScheduleService.upsertGeneralSchedule(
                "provider@test.local",
                "LUNES",
                new GeneralScheduleUpsertRequest(LocalTime.of(8, 0), LocalTime.of(12, 0))
        ));
    }

    @Test
    void shouldRejectInvalidDay() {
        UserAccountEntity provider = providerUser();

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));
        when(weekDayRepository.findByNombreDiaSemana("INVALIDO")).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> generalScheduleService.upsertGeneralSchedule(
                "provider@test.local",
                "INVALIDO",
                new GeneralScheduleUpsertRequest(LocalTime.of(8, 0), LocalTime.of(12, 0))
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

    private WeekDayEntity monday() {
        WeekDayEntity day = new WeekDayEntity();
        day.setIdDiaSemana(1L);
        day.setNombreDiaSemana("LUNES");
        return day;
    }
}
