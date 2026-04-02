package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.customerbooking.api.dto.CreateReservationRequest;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationResponse;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.customerbooking.infrastructure.UserRepository;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.customerbooking.infrastructure.ServicesAvailabilityRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ServicesAvailabilityRepository availabilityRepository;
    private final ServicesRepository servicesRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ServicesAvailabilityRepository availabilityRepository,
            ServicesRepository servicesRepository,
            UserRepository userRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.availabilityRepository = availabilityRepository;
        this.servicesRepository = servicesRepository;
        this.userRepository = userRepository;
    }

    public CreateReservationResponse createReservation(CreateReservationRequest request) {

        // 🔹 1. Disponibilidad
        ServiceAvailabilityEntity disponibilidad = availabilityRepository
                .findById(request.idDisponibilidadServicio())
                .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));

        if (!disponibilidad.getIdEstadoDisponibilidad().equals(5L)) {
            throw new RuntimeException("Disponibilidad no activa");
        }

        // 🔹 2. Servicio
        ServiceEntity servicio = servicesRepository
                .findById(disponibilidad.getIdServicio())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        if (!servicio.getIdEstadoServicio().equals(3L)) {
            throw new RuntimeException("Servicio no activo");
        }

        // 🔹 3. Proveedor
        UserAccountEntity proveedor = userRepository
                .findById(servicio.getIdUsuarioProveedor())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        if (!proveedor.getIdEstado().equals(1L)) {
            throw new RuntimeException("Proveedor inactivo");
        }

        // 🔹 4. Capacidad
        long reservasActuales = reservationRepository
                .countByIdDisponibilidadServicio(disponibilidad.getIdDisponibilidadServicio());

        int capacidad = servicio.getCapacidadMaximaConcurrente();

        if (reservasActuales >= capacidad) {
            throw new RuntimeException("No hay cupos disponibles");
        }

        // 🔹 5. Crear reserva
        ReservationEntity reserva = new ReservationEntity();
        reserva.setIdDisponibilidadServicio(disponibilidad.getIdDisponibilidadServicio());
        reserva.setIdUsuarioCliente(request.idUsuarioCliente());
        reserva.setIdEstadoReserva(7L); // ejemplo: PENDIENTE
        reserva.setFechaCreacionReserva(OffsetDateTime.now());

        ReservationEntity saved = reservationRepository.save(reserva);

        // 🔹 6. Response
        return new CreateReservationResponse(
                saved.getIdReserva(),
                saved.getIdDisponibilidadServicio(),
                saved.getFechaCreacionReserva(),
                "Reserva creada exitosamente"
        );
    }
}