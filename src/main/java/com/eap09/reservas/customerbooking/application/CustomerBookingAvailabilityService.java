package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.AvailabilityQueryFailedException;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.customerbooking.infrastructure.AvailableSlotProjection;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingAvailabilityRepository;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerBookingAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(CustomerBookingAvailabilityService.class);
    private static final String CLIENT_ROLE = "CLIENTE";

    private final CustomerBookingAvailabilityRepository repository;
    private final UserAccountRepository userAccountRepository;

    public CustomerBookingAvailabilityService(CustomerBookingAvailabilityRepository repository,
                                             UserAccountRepository userAccountRepository) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityQueryResult getAvailability(Long providerId,
                                                   Long serviceId,
                                                   LocalDate date,
                                                   String authenticatedUsername) {
        validateRequiredFields(providerId, serviceId, date);
        validateClientRole(authenticatedUsername);

        try {
            boolean hasValidRelation = repository.existsValidProviderServiceRelation(providerId, serviceId);
            if (!hasValidRelation) {
                return new AvailabilityQueryResult(
                        "No existe disponibilidad para la seleccion realizada",
                        List.of());
            }

            List<AvailabilityResponse> availabilities = repository.findReservableAvailabilities(providerId, serviceId, date)
                    .stream()
                    .map(this::toResponse)
                    .toList();

            if (availabilities.isEmpty()) {
                return new AvailabilityQueryResult(
                        "No hay disponibilidad para reserva en la fecha seleccionada",
                        List.of());
            }

            return new AvailabilityQueryResult("Consulta de horarios y cupos exitosa", availabilities);
        } catch (DataAccessException ex) {
            log.error("Error de datos al consultar horarios y cupos", ex);
            throw new AvailabilityQueryFailedException("No fue posible obtener la disponibilidad. Intenta nuevamente mas tarde");
        } catch (RuntimeException ex) {
            log.error("Error inesperado al consultar horarios y cupos", ex);
            throw new AvailabilityQueryFailedException("No fue posible obtener la disponibilidad. Intenta nuevamente mas tarde");
        }
    }

    private void validateRequiredFields(Long providerId, Long serviceId, LocalDate date) {
        if (providerId == null || serviceId == null || date == null) {
            throw new ApiException("REQUIRED_FIELDS_MISSING", "Proveedor, servicio y fecha son requeridos");
        }
    }

    private void validateClientRole(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ClientRoleRequiredException("Solo un cliente autenticado puede consultar horarios y cupos"));

        if (!CLIENT_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ClientRoleRequiredException("Solo un cliente autenticado puede consultar horarios y cupos");
        }
    }

    private AvailabilityResponse toResponse(AvailableSlotProjection availability) {
        return new AvailabilityResponse(
                availability.getAvailabilityId(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getRemainingSlots());
    }
}