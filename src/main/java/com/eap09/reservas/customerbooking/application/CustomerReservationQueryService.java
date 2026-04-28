package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.CustomerReservationQueryFailedException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.customerbooking.api.dto.CustomerReservationResponse;
import com.eap09.reservas.customerbooking.infrastructure.CustomerReservationProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerReservationQueryService {

    private static final Logger log = LoggerFactory.getLogger(CustomerReservationQueryService.class);
    private static final String CLIENT_ROLE = "CLIENTE";

    private final UserAccountRepository userAccountRepository;
    private final ReservationRepository reservationRepository;
    private final SystemEventPublisher systemEventPublisher;

    public CustomerReservationQueryService(UserAccountRepository userAccountRepository,
                                           ReservationRepository reservationRepository,
                                           SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.reservationRepository = reservationRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional(readOnly = true)
    public CustomerReservationQueryResult getOwnBookings(String authenticatedUsername) {
        UserAccountEntity customer = resolveAuthenticatedClient(authenticatedUsername);

        try {
            List<CustomerReservationResponse> bookings = reservationRepository.findByCustomerUserId(customer.getIdUsuario())
                    .stream()
                    .map(this::toResponse)
                    .toList();

            String message = bookings.isEmpty()
                    ? "No existen reservas asociadas a tu cuenta"
                    : "Consulta de reservas del cliente exitosa";

            publishQueryEvent(customer.getIdUsuario(), "EXITO", "Consulta de reservas del cliente completada. resultados=" + bookings.size());
            return new CustomerReservationQueryResult(message, bookings);
        } catch (DataAccessException ex) {
            publishQueryEventSafely(customer.getIdUsuario(), "FALLO", "No fue posible completar la consulta de reservas del cliente");
            log.error("Error de datos al consultar reservas del cliente {}", customer.getIdUsuario(), ex);
            throw new CustomerReservationQueryFailedException(
                    "No fue posible completar la consulta de reservas. Intenta nuevamente mas tarde");
        } catch (RuntimeException ex) {
            publishQueryEventSafely(customer.getIdUsuario(), "FALLO", ex.getMessage());
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedClient(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ClientRoleRequiredException("Solo un cliente autenticado puede consultar sus reservas"));

        if (!CLIENT_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ClientRoleRequiredException("Solo un cliente autenticado puede consultar sus reservas");
        }

        return user;
    }

    private CustomerReservationResponse toResponse(CustomerReservationProjection projection) {
        return new CustomerReservationResponse(
                projection.getBookingId(),
                projection.getServiceId(),
                projection.getServiceName(),
                projection.getProviderId(),
                projection.getProviderFullName(),
                projection.getSlotDate(),
                projection.getStartTime(),
                projection.getEndTime(),
                projection.getBookingStatus(),
                projection.getCreatedAt());
    }

    private void publishQueryEvent(Long customerUserId, String result, String detail) {
        systemEventPublisher.publish(SystemEvent.now(
                "CONSULTA_RESERVAS_CLIENTE",
                "tbl_reserva",
                String.valueOf(customerUserId),
                String.valueOf(customerUserId),
                result,
                detail,
                TraceIdUtil.currentTraceId()));
    }

    private void publishQueryEventSafely(Long customerUserId, String result, String detail) {
        try {
            publishQueryEvent(customerUserId, result, detail);
        } catch (RuntimeException publishEx) {
            log.warn("No fue posible registrar evento de consulta de reservas cliente {}", customerUserId, publishEx);
        }
    }
}