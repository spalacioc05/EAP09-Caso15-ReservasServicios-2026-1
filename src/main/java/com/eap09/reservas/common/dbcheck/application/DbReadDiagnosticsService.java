package com.eap09.reservas.common.dbcheck.application;

import com.eap09.reservas.common.dbcheck.infrastructure.DbReadDiagnosticsRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DbReadDiagnosticsService {

    private static final List<String> REQUIRED_TABLES = List.of(
            "tbl_rol",
            "tbl_categoria_estado",
            "tbl_estado",
            "tbl_dia_semana",
            "tbl_usuario",
            "tbl_horario_general_proveedor",
            "tbl_servicio",
            "tbl_disponibilidad_servicio",
            "tbl_reserva",
            "tbl_tipo_evento",
            "tbl_tipo_registro",
            "tbl_evento"
    );

    private final DbReadDiagnosticsRepository repository;

    public DbReadDiagnosticsService(DbReadDiagnosticsRepository repository) {
        this.repository = repository;
    }

    public DbReadDiagnosticsReport runFullReadDiagnostics(LocalDate availabilityDate) {
        return new DbReadDiagnosticsReport(
                repository.getTableExistence(REQUIRED_TABLES),
                repository.findAllRoles(),
                repository.findAllStateCategories(),
                repository.findAllStatesWithCategory(),
                repository.findAllWeekDays(),
                repository.findAllEventTypes(),
                repository.findAllRecordTypes(),
                repository.findAllUsersWithRoleAndState(),
                repository.findUsersByRole("CLIENTE"),
                repository.findUsersByRole("PROVEEDOR"),
                repository.existsUsersByRole("ADMINISTRADOR"),
                repository.findActiveUsers(),
                repository.findUsersWithTemporaryRestriction(),
                repository.findProviderGeneralSchedule(),
                repository.findServicesWithProviderAndState(),
                repository.findAvailabilitiesWithServiceProviderAndState(),
                repository.findEnabledAvailabilities(),
                repository.findAvailabilitiesByDate(availabilityDate),
                repository.findReservationsDetailed(),
                repository.findCreatedReservations(),
                repository.findCustomerReservationAvailabilityServiceProviderState(),
                repository.findProviderServiceAvailabilityState()
        );
    }
}
