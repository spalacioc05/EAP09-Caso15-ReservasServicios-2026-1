package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.customerbooking.infrastructure.CustomerReservationProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerReservationQueryServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private CustomerReservationQueryService service;

    @Test
    void shouldReturnOwnBookings() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("customer@test.local"))
                .thenReturn(Optional.of(clientUser()));
        when(reservationRepository.findByCustomerUserId(50L))
                .thenReturn(List.of(projection(700L, "CREADA"), projection(701L, "CANCELADA")));

        CustomerReservationQueryResult result = service.getOwnBookings("customer@test.local");

        assertEquals("Consulta de reservas del cliente exitosa", result.message());
        assertEquals(2, result.bookings().size());
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldReturnControlledMessageWhenNoBookings() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("customer@test.local"))
                .thenReturn(Optional.of(clientUser()));
        when(reservationRepository.findByCustomerUserId(50L)).thenReturn(List.of());

        CustomerReservationQueryResult result = service.getOwnBookings("customer@test.local");

        assertEquals("No existen reservas asociadas a tu cuenta", result.message());
        assertEquals(0, result.bookings().size());
    }

    @Test
    void shouldRejectWhenUserIsNotClient() {
        UserAccountEntity provider = clientUser();
        provider.getRol().setNombreRol("PROVEEDOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("customer@test.local"))
                .thenReturn(Optional.of(provider));

        assertThrows(ClientRoleRequiredException.class,
                () -> service.getOwnBookings("customer@test.local"));
    }

    private UserAccountEntity clientUser() {
        RoleEntity role = new RoleEntity();
        role.setNombreRol("CLIENTE");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(50L);
        user.setCorreoUsuario("customer@test.local");
        user.setRol(role);
        return user;
    }

    private CustomerReservationProjection projection(Long bookingId, String status) {
        return new CustomerReservationProjection() {
            @Override
            public Long getBookingId() {
                return bookingId;
            }

            @Override
            public Long getServiceId() {
                return 300L;
            }

            @Override
            public String getServiceName() {
                return "Servicio HU19";
            }

            @Override
            public Long getProviderId() {
                return 10L;
            }

            @Override
            public String getProviderFullName() {
                return "Proveedor Uno";
            }

            @Override
            public LocalDate getSlotDate() {
                return LocalDate.of(2026, 6, 1);
            }

            @Override
            public LocalTime getStartTime() {
                return LocalTime.of(9, 0);
            }

            @Override
            public LocalTime getEndTime() {
                return LocalTime.of(10, 0);
            }

            @Override
            public String getBookingStatus() {
                return status;
            }

            @Override
            public OffsetDateTime getCreatedAt() {
                return OffsetDateTime.now();
            }
        };
    }
}