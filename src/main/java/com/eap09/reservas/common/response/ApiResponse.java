package com.eap09.reservas.common.response;

public record ApiResponse<T>(
        String message,
        T data,
        String traceId
) {
}
