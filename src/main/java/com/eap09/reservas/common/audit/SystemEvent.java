package com.eap09.reservas.common.audit;

import java.time.OffsetDateTime;

public record SystemEvent(
        String type,
        String entityType,
        String entityId,
        String result,
        String details,
        String traceId,
        OffsetDateTime occurredAt
) {
    public static SystemEvent now(String type,
                                  String entityType,
                                  String entityId,
                                  String result,
                                  String details,
                                  String traceId) {
        return new SystemEvent(type, entityType, entityId, result, details, traceId, OffsetDateTime.now());
    }
}
