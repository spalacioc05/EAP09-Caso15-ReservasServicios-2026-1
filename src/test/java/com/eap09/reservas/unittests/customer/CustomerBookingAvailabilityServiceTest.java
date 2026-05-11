package com.eap09.reservas.unittests.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.customerbooking.application.AvailabilityQueryResult;
import com.eap09.reservas.customerbooking.application.CustomerBookingAvailabilityService;
import com.eap09.reservas.customerbooking.application.CustomerBookingOfferService;
import com.eap09.reservas.customerbooking.infrastructure.AvailableOfferProjection;
import com.eap09.reservas.customerbooking.infrastructure.AvailableSlotProjection;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingAvailabilityRepository;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingServiceRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;

public class CustomerBookingAvailabilityServiceTest {
    @Mock
    private CustomerBookingServiceRepository repository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CustomerBookingOfferService service;

    @Mock
    private CustomerBookingAvailabilityRepository availabilityRepository;

    @InjectMocks
    private CustomerBookingAvailabilityService availabilityService;

    private RoleEntity role;
    private UserAccountEntity user;
    private ServiceEntity mockService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        role = new RoleEntity();
        role.setIdRol(1L);
        role.setNombreRol("CLIENTE");

        user = new UserAccountEntity();
        user.setIdUsuario(1L);
        user.setCorreoUsuario("juan.empresa@gmail.com");
        user.setRol(role);
        user.setIdEstado(1L);

        mockService = new ServiceEntity();
        mockService.setIdServicio(200L);
        mockService.setIdUsuarioProveedor(10L);
    }

    @Test
    void getAvailableOffersForClient_Success() {

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.empresa@gmail.com"))
                .thenReturn(Optional.of(user));
        when(repository.findAvailableOffers()).thenReturn(List.of(
                projection(1L, "Servicio Uno", "Descripcion", "Proveedor Uno")));

        List<OfferResponse> offers = service.getAvailableOffers("juan.empresa@gmail.com");

        assertEquals(1, offers.size());
        assertEquals("Servicio Uno", offers.get(0).serviceName());
        assertEquals("Proveedor Uno", offers.get(0).providerName());
    }

    @Test
    void getEmptyListWhenNoOffersAvailable_Success() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.empresa@gmail.com"))
                .thenReturn(Optional.of(user));
        when(repository.findAvailableOffers()).thenReturn(List.of());

        List<OfferResponse> offers = service.getAvailableOffers("juan.empresa@gmail.com");

        assertEquals(0, offers.size());
    }

    @Test
    void getReservableSlotsForClient_Success() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.empresa@gmail.com"))
                .thenReturn(Optional.of(user));
        when(availabilityRepository.existsValidProviderServiceRelation(10L, 200L)).thenReturn(true);
        when(availabilityRepository.findReservableAvailabilities(10L, 200L, LocalDate.of(2026, 4, 20)))
                .thenReturn(List.of(slot(100L, LocalTime.of(9, 0), LocalTime.of(10, 0), 2)));

        AvailabilityQueryResult result = availabilityService.getAvailability(10L, 200L, LocalDate.of(2026, 4, 20),
                "juan.empresa@gmail.com");

        assertEquals("Consulta de horarios y cupos exitosa", result.message());
        assertEquals(1, result.availabilities().size());
        AvailabilityResponse response = result.availabilities().get(0);
        assertEquals(100L, response.availabilityId());
        assertEquals(2, response.remainingSlots());
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

    private AvailableSlotProjection slot(Long id, LocalTime start, LocalTime end, long remainingSlots) {
        return new AvailableSlotProjection() {
            @Override
            public Long getAvailabilityId() {
                return id;
            }

            @Override
            public LocalTime getStartTime() {
                return start;
            }

            @Override
            public LocalTime getEndTime() {
                return end;
            }

            @Override
            public long getRemainingSlots() {
                return remainingSlots;
            }
        };
    }
}
