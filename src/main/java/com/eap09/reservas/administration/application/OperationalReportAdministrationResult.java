package com.eap09.reservas.administration.application;

import com.eap09.reservas.administration.api.dto.OperationalReportResponse;

public record OperationalReportAdministrationResult(
        String message,
        OperationalReportResponse report
) {
}