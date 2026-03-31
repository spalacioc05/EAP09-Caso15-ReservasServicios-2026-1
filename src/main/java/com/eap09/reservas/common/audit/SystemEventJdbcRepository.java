package com.eap09.reservas.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SystemEventJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SystemEventJdbcRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void save(SystemEvent event) {
        String detailJson = toJson(Map.of("detail", event.details()));
        Long affectedRecordId = parseLongOrNull(event.entityId());

        String sql = """
                INSERT INTO tbl_evento (
                    id_tipo_evento,
                    id_tipo_registro,
                    id_estado_evento,
                    id_usuario_responsable,
                    id_registro_afectado,
                    trace_id,
                    detalle_evento
                )
                VALUES (
                    (SELECT id_tipo_evento FROM tbl_tipo_evento WHERE nombre_tipo_evento = :eventType),
                    (SELECT id_tipo_registro FROM tbl_tipo_registro WHERE nombre_tipo_registro = :entityType),
                    (
                      SELECT e.id_estado
                      FROM tbl_estado e
                      JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                      WHERE c.nombre_categoria_estado = 'tbl_evento'
                        AND e.nombre_estado = :result
                    ),
                    :responsibleUserId,
                    :affectedRecordId,
                    :traceId,
                    CAST(:detail AS jsonb)
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("eventType", event.type())
                .addValue("entityType", event.entityType())
                .addValue("result", event.result())
                .addValue("responsibleUserId", affectedRecordId)
                .addValue("affectedRecordId", affectedRecordId)
                .addValue("traceId", event.traceId() == null ? "no-trace-id" : event.traceId())
                .addValue("detail", detailJson);

        jdbc.update(sql, params);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{\"detail\":\"serialization_error\"}";
        }
    }

    private Long parseLongOrNull(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
