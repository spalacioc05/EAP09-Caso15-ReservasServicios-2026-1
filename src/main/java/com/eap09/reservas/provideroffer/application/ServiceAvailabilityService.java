package com.eap09.reservas.provideroffer.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.AvailabilityOverlapException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.util.TraceIdUtil;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceAvailabilityService {

    private static final String PROVIDER_ROLE = "PROVEEDOR";
    private static final String AVAILABILITY_STATE_CATEGORY = "tbl_disponibilidad_servicio";
    private static final String ENABLED_STATE = "HABILITADA";
    private static final String BLOCKED_STATE = "BLOQUEADA";

    private final UserAccountRepository userAccountRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceAvailabilityRepository serviceAvailabilityRepository;
    private final GeneralScheduleRepository generalScheduleRepository;
    private final WeekDayRepository weekDayRepository;
    private final StateRepository stateRepository;
    private final SystemEventPublisher systemEventPublisher;

    public ServiceAvailabilityService(UserAccountRepository userAccountRepository,
                                      ServiceRepository serviceRepository,
                                      ServiceAvailabilityRepository serviceAvailabilityRepository,
                                      GeneralScheduleRepository generalScheduleRepository,
                                      WeekDayRepository weekDayRepository,
                                      StateRepository stateRepository,
                                      SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.serviceRepository = serviceRepository;
        this.serviceAvailabilityRepository = serviceAvailabilityRepository;
        this.generalScheduleRepository = generalScheduleRepository;
        this.weekDayRepository = weekDayRepository;
        this.stateRepository = stateRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public ServiceAvailabilityResponse createAvailability(String authenticatedUsername,
                                                          Long serviceId,
                                                          ServiceAvailabilityCreateRequest request) {
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);
        ServiceEntity targetService = resolveServiceForProvider(provider.getIdUsuario(), serviceId);

        validateRange(request);

        WeekDayEntity weekDay = resolveWeekDayByDate(request.fecha());
        GeneralScheduleEntity generalSchedule = generalScheduleRepository
                .findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(provider.getIdUsuario(), weekDay.getIdDiaSemana())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "OUTSIDE_GENERAL_SCHEDULE",
                        "La franja no es valida dentro del horario general del proveedor"));

        if (request.horaInicio().isBefore(generalSchedule.getHoraInicio())
                || request.horaFin().isAfter(generalSchedule.getHoraFin())) {
            throw new ApiException(
                    "OUTSIDE_GENERAL_SCHEDULE",
                    "La franja no es valida dentro del horario general del proveedor");
        }

        boolean overlaps = serviceAvailabilityRepository.existsOverlappingRange(
                targetService.getIdServicio(),
                request.fecha(),
                request.horaInicio(),
                request.horaFin());

        if (overlaps) {
            throw new AvailabilityOverlapException("La franja propuesta se superpone con una disponibilidad existente");
        }

        StateEntity enabledState = stateRepository.findByCategoryAndStateName(AVAILABILITY_STATE_CATEGORY, ENABLED_STATE)
                .orElseThrow(() -> new IllegalStateException("Required state HABILITADA for tbl_disponibilidad_servicio was not found"));

        OffsetDateTime now = OffsetDateTime.now();
        ServiceAvailabilityEntity entity = new ServiceAvailabilityEntity();
        entity.setIdServicio(targetService.getIdServicio());
        entity.setIdEstadoDisponibilidad(enabledState.getIdEstado());
        entity.setFechaDisponibilidad(request.fecha());
        entity.setHoraInicio(request.horaInicio());
        entity.setHoraFin(request.horaFin());
        entity.setFechaCreacionDisponibilidad(now);
        entity.setFechaActualizacionDisponibilidad(now);

        ServiceAvailabilityEntity saved = serviceAvailabilityRepository.save(entity);

        systemEventPublisher.publish(SystemEvent.now(
                "CREACION_DISPONIBILIDAD",
                "tbl_disponibilidad_servicio",
                String.valueOf(provider.getIdUsuario()),
                "EXITO",
                "Disponibilidad creada para el servicio " + targetService.getIdServicio(),
                TraceIdUtil.currentTraceId()));

        return new ServiceAvailabilityResponse(
                saved.getIdDisponibilidadServicio(),
                saved.getIdServicio(),
                saved.getFechaDisponibilidad(),
                saved.getHoraInicio(),
                saved.getHoraFin(),
                enabledState.getNombreEstado());
    }

    @Transactional
    public ServiceAvailabilityResponse blockAvailability(String authenticatedUsername,
                                                         Long serviceId,
                                                         Long availabilityId) {
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);
        ServiceEntity targetService = resolveServiceForProvider(provider.getIdUsuario(), serviceId);

        ServiceAvailabilityEntity availability = serviceAvailabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AVAILABILITY_NOT_FOUND",
                        "La disponibilidad indicada no existe"));

        if (!availability.getIdServicio().equals(targetService.getIdServicio())) {
            throw new AccessDeniedException("No tiene permisos para bloquear esta disponibilidad");
        }

        StateEntity blockedState = stateRepository.findByCategoryAndStateName(AVAILABILITY_STATE_CATEGORY, BLOCKED_STATE)
                .orElseThrow(() -> new IllegalStateException("Required state BLOQUEADA for tbl_disponibilidad_servicio was not found"));

        availability.setIdEstadoDisponibilidad(blockedState.getIdEstado());
        availability.setFechaActualizacionDisponibilidad(OffsetDateTime.now());

        ServiceAvailabilityEntity saved = serviceAvailabilityRepository.save(availability);

        systemEventPublisher.publish(SystemEvent.now(
                "BLOQUEO_DISPONIBILIDAD",
                "tbl_disponibilidad_servicio",
                String.valueOf(provider.getIdUsuario()),
                "EXITO",
                "Disponibilidad bloqueada para el servicio " + targetService.getIdServicio(),
                TraceIdUtil.currentTraceId()));

        return new ServiceAvailabilityResponse(
                saved.getIdDisponibilidadServicio(),
                saved.getIdServicio(),
                saved.getFechaDisponibilidad(),
                saved.getHoraInicio(),
                saved.getHoraFin(),
                blockedState.getNombreEstado());
    }

    private UserAccountEntity resolveAuthenticatedProvider(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ProviderRoleRequiredException("Solo un proveedor autenticado puede gestionar disponibilidad"));

        if (!PROVIDER_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ProviderRoleRequiredException("Solo un proveedor autenticado puede gestionar disponibilidad");
        }

        return user;
    }

    private ServiceEntity resolveServiceForProvider(Long providerUserId, Long serviceId) {
        ServiceEntity service = serviceRepository.findByIdServicio(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SERVICE_NOT_FOUND",
                        "El servicio indicado no existe"));

        if (!service.getIdUsuarioProveedor().equals(providerUserId)) {
            throw new AccessDeniedException("No tiene permisos para operar este servicio");
        }

        return service;
    }

    private void validateRange(ServiceAvailabilityCreateRequest request) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new ApiException("INVALID_TIME_RANGE", "La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    private WeekDayEntity resolveWeekDayByDate(LocalDate date) {
        String dayName = toSpanishUppercaseDay(date.getDayOfWeek());
        return weekDayRepository.findByNombreDiaSemana(dayName)
                .orElseThrow(() -> new ApiException("INVALID_DAY_OF_WEEK", "El dia de la semana ingresado no es valido"));
    }

    private String toSpanishUppercaseDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }
}