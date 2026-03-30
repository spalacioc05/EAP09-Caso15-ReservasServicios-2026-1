package com.eap09.reservas.common.response;

import java.util.List;

public record ErrorResponse(
        String errorCode,
        String message,
        List<String> details,
        String traceId
) {
}
