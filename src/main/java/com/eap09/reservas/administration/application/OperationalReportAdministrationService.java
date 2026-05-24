package com.eap09.reservas.administration.application;

import com.eap09.reservas.administration.api.dto.BookingsByProviderResponse;
import com.eap09.reservas.administration.api.dto.BookingsByServiceResponse;
import com.eap09.reservas.administration.api.dto.BookingsByStatusResponse;
import com.eap09.reservas.administration.api.dto.OperationalReportResponse;
import com.eap09.reservas.administration.infrastructure.BookingsByProviderProjection;
import com.eap09.reservas.administration.infrastructure.BookingsByServiceProjection;
import com.eap09.reservas.administration.infrastructure.BookingsByStatusProjection;
import com.eap09.reservas.administration.infrastructure.OperationalReportRepository;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.OperationalReportGenerationFailedException;
import com.eap09.reservas.common.exception.OperationalReportUnavailableException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalReportAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(OperationalReportAdministrationService.class);

    private static final String ADMIN_ROLE = "ADMINISTRADOR";
    private static final String CREATED_STATUS = "CREADA";
    private static final String CANCELLED_STATUS = "CANCELADA";
    private static final String FINISHED_STATUS = "FINALIZADA";
    private static final String RESERVATION_ENTITY_TYPE = "tbl_reserva";
    private static final String REPORT_EVENT = "GENERACION_REPORTE_OPERATIVO";
    private static final String ADMIN_REQUIRED_MESSAGE =
            "Solo un administrador autenticado puede generar reportes operativos globales";
    private static final String SUCCESS_MESSAGE = "Reporte operativo generado correctamente";
    private static final String NO_HISTORY_MESSAGE =
            "No hay reservas registradas para generar el reporte operativo";
    private static final String INVALID_DATE_RANGE_MESSAGE = "El rango de fechas no es valido";
    private static final String REPORT_FAILED_MESSAGE =
            "No fue posible generar el reporte operativo. Intenta nuevamente mas tarde";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final UserAccountRepository userAccountRepository;
    private final OperationalReportRepository operationalReportRepository;
    private final SystemEventPublisher systemEventPublisher;

    public OperationalReportAdministrationService(UserAccountRepository userAccountRepository,
                                                  OperationalReportRepository operationalReportRepository,
                                                  SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.operationalReportRepository = operationalReportRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional(readOnly = true, noRollbackFor = {
            AdminRoleRequiredException.class,
            ApiException.class,
            OperationalReportUnavailableException.class
    })
    public OperationalReportAdministrationResult generateOperationalReport(String authenticatedUsername,
                                                                           LocalDate from,
                                                                           LocalDate to) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        UserAccountEntity adminUser = resolveAuthenticatedAdmin(authenticatedUsername);

        try {
            validateDateRange(from, to);

            long totalBookings = operationalReportRepository.countBookings(from, to);
            ensureReportHistoryExists(totalBookings);

            List<BookingsByStatusProjection> bookingsByStatus = operationalReportRepository.countBookingsByStatus(from, to);
            Map<String, Long> statusTotals = buildStatusTotals(bookingsByStatus);

            long activeBookings = statusTotals.getOrDefault(CREATED_STATUS, 0L);
            long completedBookings = statusTotals.getOrDefault(FINISHED_STATUS, 0L);
            long cancelledBookings = statusTotals.getOrDefault(CANCELLED_STATUS, 0L);
            long nonCancelledBookings = activeBookings + completedBookings;
            long totalOfferedCapacity = operationalReportRepository.calculateTotalOfferedCapacity(from, to);

            OperationalReportResponse response = new OperationalReportResponse(
                    from,
                    to,
                    totalBookings,
                    activeBookings,
                    completedBookings,
                    cancelledBookings,
                    calculateRate(cancelledBookings, totalBookings),
                    calculateRate(completedBookings, totalBookings),
                    calculateOccupancyRate(nonCancelledBookings, totalOfferedCapacity),
                    buildStatusResponses(statusTotals),
                    buildServiceResponses(operationalReportRepository.countBookingsByService(from, to)),
                    buildProviderResponses(operationalReportRepository.countBookingsByProvider(from, to)));

            publishResultEventSafely(
                    adminUser.getIdUsuario(),
                    "EXITO",
                    buildSuccessDetail(response));

            return new OperationalReportAdministrationResult(SUCCESS_MESSAGE, response);
        } catch (DataAccessException ex) {
            publishResultEventSafely(adminUser.getIdUsuario(), "FALLO", "No fue posible completar la generacion del reporte operativo");
            log.error("Error de datos al generar reporte operativo global", ex);
            throw new OperationalReportGenerationFailedException(REPORT_FAILED_MESSAGE);
        } catch (RuntimeException ex) {
            publishResultEventSafely(adminUser.getIdUsuario(), "FALLO", resolveFailureDetail(ex));
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

    private void ensureReportHistoryExists(long totalBookings) {
        if (totalBookings == 0L) {
            throw new OperationalReportUnavailableException(NO_HISTORY_MESSAGE);
        }
    }

    private Map<String, Long> buildStatusTotals(List<BookingsByStatusProjection> projections) {
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put(CREATED_STATUS, 0L);
        totals.put(FINISHED_STATUS, 0L);
        totals.put(CANCELLED_STATUS, 0L);

        for (BookingsByStatusProjection projection : projections) {
            totals.put(projection.getStatus(), projection.getTotal());
        }

        return totals;
    }

    private List<BookingsByStatusResponse> buildStatusResponses(Map<String, Long> statusTotals) {
        return statusTotals.entrySet().stream()
                .map(entry -> new BookingsByStatusResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<BookingsByServiceResponse> buildServiceResponses(List<BookingsByServiceProjection> projections) {
        return projections.stream()
                .map(projection -> new BookingsByServiceResponse(
                        projection.getServiceId(),
                        projection.getServiceName(),
                        projection.getTotal()))
                .toList();
    }

    private List<BookingsByProviderResponse> buildProviderResponses(List<BookingsByProviderProjection> projections) {
        return projections.stream()
                .map(projection -> new BookingsByProviderResponse(
                        projection.getProviderId(),
                        projection.getProviderName(),
                        projection.getTotal()))
                .toList();
    }

    private BigDecimal calculateRate(long numerator, long denominator) {
        if (denominator == 0L) {
            return ZERO_RATE;
        }

        return BigDecimal.valueOf(numerator)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOccupancyRate(long nonCancelledBookings, long totalOfferedCapacity) {
        if (totalOfferedCapacity == 0L) {
            return ZERO_RATE;
        }

        return calculateRate(nonCancelledBookings, totalOfferedCapacity);
    }

    private String buildSuccessDetail(OperationalReportResponse response) {
        return "Reporte operativo from=" + response.from()
                + " to=" + response.to()
                + " totalBookings=" + response.totalBookings()
                + " cancellationRate=" + response.cancellationRate()
                + " completionRate=" + response.completionRate()
                + " occupancyRate=" + response.occupancyRate();
    }

    private String resolveFailureDetail(RuntimeException ex) {
        if (ex instanceof ApiException
                || ex instanceof AdminRoleRequiredException
                || ex instanceof OperationalReportUnavailableException) {
            return ex.getMessage();
        }

        return "No fue posible completar la generacion del reporte operativo";
    }

    private void publishResultEvent(Long responsibleUserId, String result, String details) {
        systemEventPublisher.publish(SystemEvent.now(
                REPORT_EVENT,
                RESERVATION_ENTITY_TYPE,
                String.valueOf(responsibleUserId),
                String.valueOf(responsibleUserId),
                result,
                details,
                TraceIdUtil.currentTraceId()));
    }

    private void publishResultEventSafely(Long responsibleUserId, String result, String details) {
        try {
            publishResultEvent(responsibleUserId, result, details);
        } catch (RuntimeException publishFailure) {
            log.warn("No fue posible registrar evento de reporte operativo para el administrador {}", responsibleUserId, publishFailure);
        }
    }
}