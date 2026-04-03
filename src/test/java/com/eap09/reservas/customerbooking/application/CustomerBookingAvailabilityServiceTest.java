package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.AvailabilityQueryFailedException;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.customerbooking.infrastructure.AvailableSlotProjection;
import com.eap09.reservas.customerbooking.infrastructure.CustomerBookingAvailabilityRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class CustomerBookingAvailabilityServiceTest {

    @Mock
    private CustomerBookingAvailabilityRepository repository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CustomerBookingAvailabilityService service;

    @Test
    void shouldReturnReservableSlotsForClient() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(user("client@test.local", "CLIENTE")));
        when(repository.existsValidProviderServiceRelation(10L, 20L)).thenReturn(true);
        when(repository.findReservableAvailabilities(10L, 20L, LocalDate.of(2026, 4, 20)))
                .thenReturn(List.of(slot(100L, LocalTime.of(9, 0), LocalTime.of(10, 0), 2)));

        AvailabilityQueryResult result = service.getAvailability(10L, 20L, LocalDate.of(2026, 4, 20), "client@test.local");

        assertEquals("Consulta de horarios y cupos exitosa", result.message());
        assertEquals(1, result.availabilities().size());
        AvailabilityResponse response = result.availabilities().get(0);
        assertEquals(100L, response.availabilityId());
        assertEquals(2, response.remainingSlots());
    }

    @Test
    void shouldReturnRelationInvalidMessageWhenProviderServiceIsInvalid() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(user("client@test.local", "CLIENTE")));
        when(repository.existsValidProviderServiceRelation(10L, 20L)).thenReturn(false);

        AvailabilityQueryResult result = service.getAvailability(10L, 20L, LocalDate.of(2026, 4, 20), "client@test.local");

        assertEquals("No existe disponibilidad para la seleccion realizada", result.message());
        assertEquals(0, result.availabilities().size());
    }

    @Test
    void shouldReturnNoAvailabilityMessageWhenDateHasNoReservableSlots() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(user("client@test.local", "CLIENTE")));
        when(repository.existsValidProviderServiceRelation(10L, 20L)).thenReturn(true);
        when(repository.findReservableAvailabilities(10L, 20L, LocalDate.of(2026, 4, 20)))
                .thenReturn(List.of());

        AvailabilityQueryResult result = service.getAvailability(10L, 20L, LocalDate.of(2026, 4, 20), "client@test.local");

        assertEquals("No hay disponibilidad para reserva en la fecha seleccionada", result.message());
        assertEquals(0, result.availabilities().size());
    }

    @Test
    void shouldFailWhenRequiredFieldsAreMissing() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getAvailability(10L, null, LocalDate.of(2026, 4, 20), "client@test.local"));

        assertEquals("REQUIRED_FIELDS_MISSING", ex.getErrorCode());
        assertEquals("Proveedor, servicio y fecha son requeridos", ex.getMessage());
    }

    @Test
    void shouldRejectNonClientRole() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(user("provider@test.local", "PROVEEDOR")));

        ClientRoleRequiredException ex = assertThrows(ClientRoleRequiredException.class,
                () -> service.getAvailability(10L, 20L, LocalDate.of(2026, 4, 20), "provider@test.local"));
        assertNotNull(ex);
    }

    @Test
    void shouldTranslateRepositoryErrorToControlledException() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(user("client@test.local", "CLIENTE")));
        when(repository.existsValidProviderServiceRelation(10L, 20L))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        AvailabilityQueryFailedException ex = assertThrows(AvailabilityQueryFailedException.class,
                () -> service.getAvailability(10L, 20L, LocalDate.of(2026, 4, 20), "client@test.local"));
        assertNotNull(ex);
    }

    private UserAccountEntity user(String email, String roleName) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol(roleName);

        UserAccountEntity user = new UserAccountEntity();
        user.setCorreoUsuario(email);
        user.setRol(role);
        return user;
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
