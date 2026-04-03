package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.OfferQueryFailedException;
import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.customerbooking.infrastructure.AvailableOfferProjection;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingServiceRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class CustomerBookingOfferServiceTest {

    @Mock
    private CustomerBookingServiceRepository repository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CustomerBookingOfferService service;

    @Test
    void shouldReturnAvailableOffersForClient() {
        UserAccountEntity client = user("client@test.local", "CLIENTE");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(repository.findAvailableOffers()).thenReturn(List.of(
                projection(1L, "Servicio Uno", "Descripcion", "Proveedor Uno")
        ));

        List<OfferResponse> offers = service.getAvailableOffers("client@test.local");

        assertEquals(1, offers.size());
        assertEquals("Servicio Uno", offers.get(0).serviceName());
        assertEquals("Proveedor Uno", offers.get(0).providerName());
    }

    @Test
    void shouldReturnEmptyListWhenNoOffersAvailable() {
        UserAccountEntity client = user("client@test.local", "CLIENTE");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(repository.findAvailableOffers()).thenReturn(List.of());

        List<OfferResponse> offers = service.getAvailableOffers("client@test.local");

        assertEquals(0, offers.size());
    }

    @Test
    void shouldRejectNonClientRole() {
        UserAccountEntity provider = user("provider@test.local", "PROVEEDOR");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider));

        assertThrows(ClientRoleRequiredException.class,
                () -> service.getAvailableOffers("provider@test.local"));
    }

    @Test
    void shouldTranslateRepositoryFailureToControlledException() {
        UserAccountEntity client = user("client@test.local", "CLIENTE");
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(repository.findAvailableOffers()).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThrows(OfferQueryFailedException.class,
                () -> service.getAvailableOffers("client@test.local"));
    }

    private UserAccountEntity user(String email, String roleName) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol(roleName);

        UserAccountEntity user = new UserAccountEntity();
        user.setCorreoUsuario(email);
        user.setRol(role);
        return user;
    }

    private AvailableOfferProjection projection(Long id, String serviceName, String description, String providerName) {
        return new AvailableOfferProjection() {
            @Override
            public Long getServiceId() {
                return id;
            }

            @Override
            public String getServiceName() {
                return serviceName;
            }

            @Override
            public String getServiceDescription() {
                return description;
            }

            @Override
            public String getProviderName() {
                return providerName;
            }
        };
    }
}
