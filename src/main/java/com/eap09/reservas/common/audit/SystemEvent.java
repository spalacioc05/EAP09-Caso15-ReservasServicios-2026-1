package com.eap09.reservas.common.audit;

import java.time.OffsetDateTime;

public record SystemEvent(
        String type,
        String entityType,
        String responsibleUserId,
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
        return new SystemEvent(type, entityType, entityId, entityId, result, details, traceId, OffsetDateTime.now());
    }

    public static SystemEvent now(String type,
                                  String entityType,
                                  String responsibleUserId,
                                  String entityId,
                                  String result,
                                  String details,
                                  String traceId) {
        return new SystemEvent(type, entityType, responsibleUserId, entityId, result, details, traceId, OffsetDateTime.now());
    }
}
