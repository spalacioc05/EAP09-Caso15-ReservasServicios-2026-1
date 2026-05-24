package com.eap09.reservas.administration.application;

import com.eap09.reservas.administration.api.dto.AdminReservationSupervisionItemResponse;
import com.eap09.reservas.administration.infrastructure.AdminReservationSupervisionProjection;
import com.eap09.reservas.administration.infrastructure.AdminReservationSupervisionRepository;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminReservationQueryFailedException;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReservationSupervisionService {

    private static final Logger log = LoggerFactory.getLogger(AdminReservationSupervisionService.class);

    private static final String ADMIN_ROLE = "ADMINISTRADOR";
    private static final String RESERVATION_STATE_CATEGORY = "tbl_reserva";
    private static final String RESERVATION_ENTITY_TYPE = "tbl_reserva";
    private static final String ADMIN_BOOKING_QUERY_EVENT = "CONSULTA_ADMIN_RESERVAS";
    private static final String ADMIN_REQUIRED_MESSAGE =
            "Solo un administrador autenticado puede consultar reservas globales";
    private static final String SUCCESS_MESSAGE = "Reservas consultadas correctamente";
    private static final String NO_MATCHES_MESSAGE = "No existen reservas que cumplan con esos filtros";
    private static final String NO_RESERVATIONS_MESSAGE = "No existen reservas registradas en la plataforma";
    private static final String INVALID_DATE_RANGE_MESSAGE = "El rango de fechas no es valido";
    private static final String INVALID_STATUS_MESSAGE = "El estado de reserva solicitado no es valido";
    private static final String QUERY_FAILED_MESSAGE =
            "No fue posible consultar las reservas administrativas. Intenta nuevamente mas tarde";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final AdminReservationSupervisionRepository adminReservationSupervisionRepository;
    private final SystemEventPublisher systemEventPublisher;

    public AdminReservationSupervisionService(UserAccountRepository userAccountRepository,
                                              StateRepository stateRepository,
                                              AdminReservationSupervisionRepository adminReservationSupervisionRepository,
                                              SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.adminReservationSupervisionRepository = adminReservationSupervisionRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional(readOnly = true, noRollbackFor = {
            AdminRoleRequiredException.class,
            ApiException.class
    })
    public AdminReservationSupervisionResult getReservations(String authenticatedUsername,
                                                             Long customerId,
                                                             Long providerId,
                                                             Long serviceId,
                                                             String status,
                                                             LocalDate from,
                                                             LocalDate to) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        UserAccountEntity adminUser = resolveAuthenticatedAdmin(authenticatedUsername);

        try {
            validateDateRange(from, to);
            Long reservationStateId = resolveReservationStateId(status);

            List<AdminReservationSupervisionItemResponse> bookings = adminReservationSupervisionRepository.findReservations(
                            customerId,
                            providerId,
                            serviceId,
                            reservationStateId,
                            from,
                            to)
                    .stream()
                    .map(this::toResponse)
                    .toList();

            String message = resolveMessage(bookings);

            publishEventSafely(
                    adminUser.getIdUsuario(),
                    "EXITO",
                    buildSuccessDetail(customerId, providerId, serviceId, status, from, to, bookings.size()));

            return new AdminReservationSupervisionResult(message, bookings);
        } catch (DataAccessException ex) {
            publishEventSafely(adminUser.getIdUsuario(), "FALLO", "No fue posible completar la consulta administrativa de reservas");
            log.error("Error de datos al consultar reservas administrativas", ex);
            throw new AdminReservationQueryFailedException(QUERY_FAILED_MESSAGE);
        } catch (RuntimeException ex) {
            publishEventSafely(adminUser.getIdUsuario(), "FALLO", resolveFailureDetail(ex));
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedAdmin(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new AdminRoleRequiredException(ADMIN_REQUIRED_MESSAGE));

        if (!ADMIN_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new AdminRoleRequiredException(ADMIN_REQUIRED_MESSAGE);
        }

        return user;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException("INVALID_DATE_RANGE", INVALID_DATE_RANGE_MESSAGE);
        }
    }

    private Long resolveReservationStateId(String status) {
        if (status == null) {
            return null;
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (normalizedStatus.isBlank()) {
            throw new ApiException("INVALID_RESERVATION_STATUS", INVALID_STATUS_MESSAGE);
        }

        StateEntity state = stateRepository.findByCategoryAndStateName(RESERVATION_STATE_CATEGORY, normalizedStatus)
                .orElseThrow(() -> new ApiException("INVALID_RESERVATION_STATUS", INVALID_STATUS_MESSAGE));

        return state.getIdEstado();
    }

    private AdminReservationSupervisionItemResponse toResponse(AdminReservationSupervisionProjection projection) {
        return new AdminReservationSupervisionItemResponse(
                projection.getBookingId(),
                projection.getCustomerId(),
                projection.getCustomerFullName(),
                projection.getCustomerEmail(),
                projection.getProviderId(),
                projection.getProviderFullName(),
                projection.getProviderEmail(),
                projection.getServiceId(),
                projection.getServiceName(),
                projection.getAvailabilityId(),
                projection.getSlotDate(),
                projection.getStartTime(),
                projection.getEndTime(),
                projection.getBookingStatus(),
                projection.getCreatedAt() == null ? null : projection.getCreatedAt().atOffset(ZoneOffset.UTC),
                projection.getUpdatedAt() == null ? null : projection.getUpdatedAt().atOffset(ZoneOffset.UTC),
                projection.getCancelledAt() == null ? null : projection.getCancelledAt().atOffset(ZoneOffset.UTC),
                projection.getFinishedAt() == null ? null : projection.getFinishedAt().atOffset(ZoneOffset.UTC));
    }

    private String resolveMessage(List<AdminReservationSupervisionItemResponse> bookings) {
        if (!bookings.isEmpty()) {
            return SUCCESS_MESSAGE;
        }

        return adminReservationSupervisionRepository.existsAnyReservation()
                ? NO_MATCHES_MESSAGE
                : NO_RESERVATIONS_MESSAGE;
    }

    private String buildSuccessDetail(Long customerId,
                                      Long providerId,
                                      Long serviceId,
                                      String status,
                                      LocalDate from,
                                      LocalDate to,
                                      int results) {
        return "Consulta admin reservas customerId=" + customerId
                + " providerId=" + providerId
                + " serviceId=" + serviceId
                + " status=" + status
                + " from=" + from
                + " to=" + to
                + " resultados=" + results;
    }

    private String resolveFailureDetail(RuntimeException ex) {
        if (ex instanceof ApiException || ex instanceof AdminRoleRequiredException) {
            return ex.getMessage();
        }

        return "No fue posible completar la consulta administrativa de reservas";
    }

    private void publishEvent(Long responsibleUserId, String result, String details) {
        systemEventPublisher.publish(SystemEvent.now(
                ADMIN_BOOKING_QUERY_EVENT,
                RESERVATION_ENTITY_TYPE,
                String.valueOf(responsibleUserId),
                String.valueOf(responsibleUserId),
                result,
                details,
                TraceIdUtil.currentTraceId()));
    }

    private void publishEventSafely(Long responsibleUserId, String result, String details) {
        try {
            publishEvent(responsibleUserId, result, details);
        } catch (RuntimeException publishFailure) {
            log.warn("No fue posible registrar evento de supervision administrativa de reservas {}", responsibleUserId, publishFailure);
        }
    }
}