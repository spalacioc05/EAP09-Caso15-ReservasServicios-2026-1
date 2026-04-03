package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.OfferQueryFailedException;
import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.customerbooking.infrastructure.AvailableOfferProjection;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingServiceRepository;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerBookingOfferService {

    private static final String CLIENT_ROLE = "CLIENTE";
    private static final Logger log = LoggerFactory.getLogger(CustomerBookingOfferService.class);

    private final CustomerBookingServiceRepository repository;
    private final UserAccountRepository userAccountRepository;

    public CustomerBookingOfferService(CustomerBookingServiceRepository repository,
                                       UserAccountRepository userAccountRepository) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getAvailableOffers(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ClientRoleRequiredException("Solo un cliente autenticado puede consultar la oferta"));

        if (!CLIENT_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ClientRoleRequiredException("Solo un cliente autenticado puede consultar la oferta");
        }

        try {
            List<AvailableOfferProjection> offers = repository.findAvailableOffers();
            return offers.stream()
                    .map(offer -> new OfferResponse(
                        offer.getServiceId(),
                        offer.getServiceName(),
                        offer.getServiceDescription(),
                        offer.getProviderName()))
                    .toList();
        } catch (DataAccessException ex) {
            log.error("Error de datos al consultar oferta disponible", ex);
            throw new OfferQueryFailedException("No fue posible obtener la oferta. Intenta nuevamente mas tarde");
        } catch (RuntimeException ex) {
            log.error("Error inesperado al consultar oferta disponible", ex);
            throw new OfferQueryFailedException("No fue posible obtener la oferta. Intenta nuevamente mas tarde");
        }
    }
}