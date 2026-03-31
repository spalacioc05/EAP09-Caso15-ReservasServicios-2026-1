package com.eap09.reservas.common.dbcheck.application;

import java.util.List;
import java.util.Map;

public record DbReadDiagnosticsReport(
        Map<String, Boolean> tableExistence,
        List<Map<String, Object>> roles,
        List<Map<String, Object>> stateCategories,
        List<Map<String, Object>> statesWithCategory,
        List<Map<String, Object>> weekDays,
        List<Map<String, Object>> eventTypes,
        List<Map<String, Object>> recordTypes,
        List<Map<String, Object>> usersWithRoleAndState,
        List<Map<String, Object>> customerUsers,
        List<Map<String, Object>> providerUsers,
        boolean adminUsersExist,
        List<Map<String, Object>> activeUsers,
        List<Map<String, Object>> usersWithTemporaryRestriction,
        List<Map<String, Object>> providerGeneralSchedule,
        List<Map<String, Object>> servicesWithProviderAndState,
        List<Map<String, Object>> availabilitiesWithServiceProviderAndState,
        List<Map<String, Object>> enabledAvailabilities,
        List<Map<String, Object>> availabilitiesByDate,
        List<Map<String, Object>> reservationsDetailed,
        List<Map<String, Object>> createdReservations,
        List<Map<String, Object>> customerReservationAvailabilityServiceProviderState,
        List<Map<String, Object>> providerServiceAvailabilityState
) {
}
