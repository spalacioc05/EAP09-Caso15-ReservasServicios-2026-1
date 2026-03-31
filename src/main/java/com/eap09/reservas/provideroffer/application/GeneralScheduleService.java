package com.eap09.reservas.provideroffer.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleResponse;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleUpsertRequest;
import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import com.eap09.reservas.provideroffer.infrastructure.GeneralScheduleRepository;
import com.eap09.reservas.provideroffer.infrastructure.WeekDayRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeneralScheduleService {

    private static final String PROVIDER_ROLE = "PROVEEDOR";

    private final GeneralScheduleRepository generalScheduleRepository;
    private final WeekDayRepository weekDayRepository;
    private final UserAccountRepository userAccountRepository;
    private final SystemEventPublisher systemEventPublisher;

    public GeneralScheduleService(GeneralScheduleRepository generalScheduleRepository,
                                  WeekDayRepository weekDayRepository,
                                  UserAccountRepository userAccountRepository,
                                  SystemEventPublisher systemEventPublisher) {
        this.generalScheduleRepository = generalScheduleRepository;
        this.weekDayRepository = weekDayRepository;
        this.userAccountRepository = userAccountRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public GeneralScheduleResponse upsertGeneralSchedule(String authenticatedUsername,
                                                         String dayOfWeek,
                                                         GeneralScheduleUpsertRequest request) {
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);
        validateTimeRange(request);

        WeekDayEntity day = resolveDayOfWeek(dayOfWeek);
        List<GeneralScheduleEntity> existingSchedules = generalScheduleRepository
                .findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(provider.getIdUsuario(), day.getIdDiaSemana());

        OffsetDateTime now = OffsetDateTime.now();
        boolean replaced = !existingSchedules.isEmpty();

        GeneralScheduleEntity entity;
        if (replaced) {
            entity = existingSchedules.get(0);
            if (existingSchedules.size() > 1) {
                generalScheduleRepository.deleteAll(existingSchedules.subList(1, existingSchedules.size()));
            }
        } else {
            entity = new GeneralScheduleEntity();
            entity.setIdUsuarioProveedor(provider.getIdUsuario());
            entity.setDiaSemana(day);
            entity.setFechaCreacionHorarioGeneral(now);
        }

        entity.setHoraInicio(request.horaInicio());
        entity.setHoraFin(request.horaFin());
        entity.setFechaActualizacionHorarioGeneral(now);

        GeneralScheduleEntity saved = generalScheduleRepository.save(entity);

        systemEventPublisher.publish(SystemEvent.now(
                "DEFINICION_HORARIO_GENERAL",
                "tbl_horario_general_proveedor",
            String.valueOf(saved.getIdUsuarioProveedor()),
                "EXITO",
                replaced
                ? "Horario general reemplazado para el dia " + day.getNombreDiaSemana()
                : "Horario general definido para el dia " + day.getNombreDiaSemana(),
                TraceIdUtil.currentTraceId()));

        return new GeneralScheduleResponse(
                saved.getIdUsuarioProveedor(),
                day.getNombreDiaSemana(),
                saved.getHoraInicio(),
                saved.getHoraFin());
    }

    private UserAccountEntity resolveAuthenticatedProvider(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ProviderRoleRequiredException("Solo un proveedor autenticado puede definir el horario general"));

        if (!PROVIDER_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ProviderRoleRequiredException("Solo un proveedor autenticado puede definir el horario general");
        }

        return user;
    }

    private void validateTimeRange(GeneralScheduleUpsertRequest request) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new ApiException("INVALID_TIME_RANGE", "La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    private WeekDayEntity resolveDayOfWeek(String dayOfWeek) {
        String normalizedDay = dayOfWeek.trim().toUpperCase(Locale.ROOT);
        return weekDayRepository.findByNombreDiaSemana(normalizedDay)
                .orElseThrow(() -> new ApiException("INVALID_DAY_OF_WEEK", "El dia de la semana ingresado no es valido"));
    }
}
