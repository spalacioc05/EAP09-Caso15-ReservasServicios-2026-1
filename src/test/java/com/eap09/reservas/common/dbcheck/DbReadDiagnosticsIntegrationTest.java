package com.eap09.reservas.common.dbcheck;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eap09.reservas.common.dbcheck.application.DbReadDiagnosticsReport;
import com.eap09.reservas.common.dbcheck.application.DbReadDiagnosticsService;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.LocalDate;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class DbReadDiagnosticsIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbReadDiagnosticsIntegrationTest.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DbReadDiagnosticsService diagnosticsService;

        @Autowired
        private UserAccountRepository userAccountRepository;

    @Test
    void shouldConnectToDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.isValid(5), "Database connection is not valid");
        }
    }

    @Test
    void shouldValidateSchemaCatalogsAndMainReadQueries() {
        DbReadDiagnosticsReport report = diagnosticsService.runFullReadDiagnostics(LocalDate.now());

        assertFalse(report.tableExistence().isEmpty(), "Table existence map should not be empty");
        assertTrue(report.tableExistence().values().stream().allMatch(Boolean.TRUE::equals),
                "All required tables must exist");

        assertFalse(report.roles().isEmpty(), "Roles catalog should have data");
        assertFalse(report.stateCategories().isEmpty(), "State category catalog should have data");
        assertFalse(report.statesWithCategory().isEmpty(), "State catalog should have data");
        assertFalse(report.weekDays().isEmpty(), "Week day catalog should have data");
        assertFalse(report.eventTypes().isEmpty(), "Event types catalog should have data");
        assertFalse(report.recordTypes().isEmpty(), "Record types catalog should have data");

        Set<String> expectedStateCategories = Set.of(
                "tbl_usuario",
                "tbl_servicio",
                "tbl_disponibilidad_servicio",
                "tbl_reserva",
                "tbl_evento"
        );

        Set<String> actualCategories = report.stateCategories().stream()
                .map(row -> String.valueOf(row.get("nombre_categoria_estado")))
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(actualCategories.containsAll(expectedStateCategories),
                "State categories must include all expected table-based categories");

        assertNotNull(report.usersWithRoleAndState());
        assertNotNull(report.customerUsers());
        assertNotNull(report.providerUsers());
        assertNotNull(report.activeUsers());
        assertNotNull(report.usersWithTemporaryRestriction());
        assertNotNull(report.providerGeneralSchedule());
        assertNotNull(report.servicesWithProviderAndState());
        assertNotNull(report.availabilitiesWithServiceProviderAndState());
        assertNotNull(report.enabledAvailabilities());
        assertNotNull(report.availabilitiesByDate());
        assertNotNull(report.reservationsDetailed());
        assertNotNull(report.createdReservations());
        assertNotNull(report.customerReservationAvailabilityServiceProviderState());
        assertNotNull(report.providerServiceAvailabilityState());

        logSummary(report);
    }

        @Test
        void shouldReadUserDataThroughJpaRepository() {
                var page = userAccountRepository.findAll(PageRequest.of(0, 5));
                assertNotNull(page);
                assertTrue(page.getTotalElements() >= 0);
        }

    private void logSummary(DbReadDiagnosticsReport report) {
        LOGGER.info("dbcheck summary tables={}", report.tableExistence());
        LOGGER.info("dbcheck summary catalog_counts roles={} stateCategories={} states={} weekDays={} eventTypes={} recordTypes={}",
                report.roles().size(),
                report.stateCategories().size(),
                report.statesWithCategory().size(),
                report.weekDays().size(),
                report.eventTypes().size(),
                report.recordTypes().size());
        LOGGER.info("dbcheck summary user_counts all={} customers={} providers={} adminsExist={} active={} restricted={}",
                report.usersWithRoleAndState().size(),
                report.customerUsers().size(),
                report.providerUsers().size(),
                report.adminUsersExist(),
                report.activeUsers().size(),
                report.usersWithTemporaryRestriction().size());
        LOGGER.info("dbcheck summary domain_counts schedules={} services={} availabilities={} enabledAvailabilities={} availabilitiesByDate={} reservations={} createdReservations={}",
                report.providerGeneralSchedule().size(),
                report.servicesWithProviderAndState().size(),
                report.availabilitiesWithServiceProviderAndState().size(),
                report.enabledAvailabilities().size(),
                report.availabilitiesByDate().size(),
                report.reservationsDetailed().size(),
                report.createdReservations().size());
        LOGGER.info("dbcheck summary join_counts customerReservationJoin={} providerServiceAvailabilityJoin={}",
                report.customerReservationAvailabilityServiceProviderState().size(),
                report.providerServiceAvailabilityState().size());
    }
}
