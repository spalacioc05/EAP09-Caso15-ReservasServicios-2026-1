package com.eap09.reservas.common.dbcheck.infrastructure;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DbReadDiagnosticsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DbReadDiagnosticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Boolean> getTableExistence(List<String> tableNames) {
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (:tableNames)
                """;

        List<String> existing = jdbc.queryForList(
                sql,
                new MapSqlParameterSource("tableNames", tableNames),
                String.class);

        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String tableName : tableNames) {
            result.put(tableName, existing.contains(tableName));
        }
        return result;
    }

    public List<Map<String, Object>> findAllRoles() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT id_rol, nombre_rol, descripcion_rol
                FROM tbl_rol
                ORDER BY id_rol
                """);
    }

    public List<Map<String, Object>> findAllStateCategories() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT id_categoria_estado, nombre_categoria_estado, descripcion_categoria_estado
                FROM tbl_categoria_estado
                ORDER BY id_categoria_estado
                """);
    }

    public List<Map<String, Object>> findAllStatesWithCategory() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT e.id_estado,
                       e.nombre_estado,
                       e.descripcion_estado,
                       c.id_categoria_estado,
                       c.nombre_categoria_estado
                FROM tbl_estado e
                JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                ORDER BY c.nombre_categoria_estado, e.nombre_estado
                """);
    }

    public List<Map<String, Object>> findAllWeekDays() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT id_dia_semana, nombre_dia_semana, orden_dia_semana
                FROM tbl_dia_semana
                ORDER BY orden_dia_semana
                """);
    }

    public List<Map<String, Object>> findAllEventTypes() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT id_tipo_evento, nombre_tipo_evento, descripcion_tipo_evento
                FROM tbl_tipo_evento
                ORDER BY id_tipo_evento
                """);
    }

    public List<Map<String, Object>> findAllRecordTypes() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT id_tipo_registro, nombre_tipo_registro, descripcion_tipo_registro
                FROM tbl_tipo_registro
                ORDER BY id_tipo_registro
                """);
    }

    public List<Map<String, Object>> findAllUsersWithRoleAndState() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT u.id_usuario,
                       u.correo_usuario,
                       r.nombre_rol,
                       e.nombre_estado,
                       u.fecha_fin_restriccion_acceso
                FROM tbl_usuario u
                JOIN tbl_rol r ON r.id_rol = u.id_rol
                JOIN tbl_estado e ON e.id_estado = u.id_estado_usuario
                ORDER BY u.id_usuario
                """);
    }

    public List<Map<String, Object>> findUsersByRole(String roleName) {
        String sql = """
                SELECT u.id_usuario, u.correo_usuario, r.nombre_rol, e.nombre_estado
                FROM tbl_usuario u
                JOIN tbl_rol r ON r.id_rol = u.id_rol
                JOIN tbl_estado e ON e.id_estado = u.id_estado_usuario
                WHERE r.nombre_rol = :roleName
                ORDER BY u.id_usuario
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("roleName", roleName));
    }

    public boolean existsUsersByRole(String roleName) {
        String sql = """
                SELECT EXISTS(
                    SELECT 1
                    FROM tbl_usuario u
                    JOIN tbl_rol r ON r.id_rol = u.id_rol
                    WHERE r.nombre_rol = :roleName
                )
                """;
        Boolean exists = jdbc.queryForObject(sql, new MapSqlParameterSource("roleName", roleName), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public List<Map<String, Object>> findActiveUsers() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT u.id_usuario, u.correo_usuario, r.nombre_rol, e.nombre_estado
                FROM tbl_usuario u
                JOIN tbl_rol r ON r.id_rol = u.id_rol
                JOIN tbl_estado e ON e.id_estado = u.id_estado_usuario
                JOIN tbl_categoria_estado c ON c.id_categoria_estado = e.id_categoria_estado
                WHERE c.nombre_categoria_estado = 'tbl_usuario'
                  AND e.nombre_estado = 'ACTIVA'
                ORDER BY u.id_usuario
                """);
    }

    public List<Map<String, Object>> findUsersWithTemporaryRestriction() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT u.id_usuario,
                       u.correo_usuario,
                       u.fecha_fin_restriccion_acceso
                FROM tbl_usuario u
                WHERE u.fecha_fin_restriccion_acceso IS NOT NULL
                  AND u.fecha_fin_restriccion_acceso > NOW()
                ORDER BY u.fecha_fin_restriccion_acceso
                """);
    }

    public List<Map<String, Object>> findProviderGeneralSchedule() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT p.id_usuario AS provider_id,
                       p.correo_usuario AS provider_email,
                       d.nombre_dia_semana,
                       h.hora_inicio,
                       h.hora_fin
                FROM tbl_horario_general_proveedor h
                JOIN tbl_usuario p ON p.id_usuario = h.id_usuario_proveedor
                JOIN tbl_rol r ON r.id_rol = p.id_rol
                JOIN tbl_dia_semana d ON d.id_dia_semana = h.id_dia_semana
                WHERE r.nombre_rol = 'PROVEEDOR'
                ORDER BY p.id_usuario, d.orden_dia_semana
                """);
    }

    public List<Map<String, Object>> findServicesWithProviderAndState() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT s.id_servicio,
                       s.nombre_servicio,
                       s.duracion_minutos,
                       s.capacidad_maxima_concurrente,
                       p.id_usuario AS provider_id,
                       p.correo_usuario AS provider_email,
                       es.nombre_estado AS estado_servicio
                FROM tbl_servicio s
                JOIN tbl_usuario p ON p.id_usuario = s.id_usuario_proveedor
                JOIN tbl_estado es ON es.id_estado = s.id_estado_servicio
                ORDER BY s.id_servicio
                """);
    }

    public List<Map<String, Object>> findAvailabilitiesWithServiceProviderAndState() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT ds.id_disponibilidad_servicio,
                       s.id_servicio,
                       s.nombre_servicio,
                       p.id_usuario AS provider_id,
                       p.correo_usuario AS provider_email,
                       ds.fecha_disponibilidad,
                       ds.hora_inicio,
                       ds.hora_fin,
                       ed.nombre_estado AS estado_disponibilidad
                FROM tbl_disponibilidad_servicio ds
                JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio
                JOIN tbl_usuario p ON p.id_usuario = s.id_usuario_proveedor
                JOIN tbl_estado ed ON ed.id_estado = ds.id_estado_disponibilidad
                ORDER BY ds.fecha_disponibilidad, ds.hora_inicio
                """);
    }

    public List<Map<String, Object>> findEnabledAvailabilities() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT ds.id_disponibilidad_servicio,
                       ds.fecha_disponibilidad,
                       ds.hora_inicio,
                       ds.hora_fin,
                       ed.nombre_estado AS estado_disponibilidad
                FROM tbl_disponibilidad_servicio ds
                JOIN tbl_estado ed ON ed.id_estado = ds.id_estado_disponibilidad
                JOIN tbl_categoria_estado c ON c.id_categoria_estado = ed.id_categoria_estado
                WHERE c.nombre_categoria_estado = 'tbl_disponibilidad_servicio'
                  AND ed.nombre_estado = 'HABILITADA'
                ORDER BY ds.fecha_disponibilidad, ds.hora_inicio
                """);
    }

    public List<Map<String, Object>> findAvailabilitiesByDate(LocalDate date) {
        String sql = """
                SELECT ds.id_disponibilidad_servicio,
                       s.nombre_servicio,
                       p.correo_usuario AS provider_email,
                       ds.fecha_disponibilidad,
                       ds.hora_inicio,
                       ds.hora_fin,
                       ed.nombre_estado AS estado_disponibilidad
                FROM tbl_disponibilidad_servicio ds
                JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio
                JOIN tbl_usuario p ON p.id_usuario = s.id_usuario_proveedor
                JOIN tbl_estado ed ON ed.id_estado = ds.id_estado_disponibilidad
                WHERE ds.fecha_disponibilidad = :targetDate
                ORDER BY ds.hora_inicio
                """;

        return jdbc.queryForList(sql, new MapSqlParameterSource("targetDate", Date.valueOf(date)));
    }

    public List<Map<String, Object>> findReservationsDetailed() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT r.id_reserva,
                       c.id_usuario AS client_id,
                       c.correo_usuario AS client_email,
                       p.id_usuario AS provider_id,
                       p.correo_usuario AS provider_email,
                       s.id_servicio,
                       s.nombre_servicio,
                       ds.fecha_disponibilidad,
                       ds.hora_inicio,
                       ds.hora_fin,
                       er.nombre_estado AS estado_reserva
                FROM tbl_reserva r
                JOIN tbl_usuario c ON c.id_usuario = r.id_usuario_cliente
                JOIN tbl_disponibilidad_servicio ds ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
                JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio
                JOIN tbl_usuario p ON p.id_usuario = s.id_usuario_proveedor
                JOIN tbl_estado er ON er.id_estado = r.id_estado_reserva
                ORDER BY r.id_reserva
                """);
    }

    public List<Map<String, Object>> findCreatedReservations() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT r.id_reserva,
                       r.fecha_creacion_reserva,
                       er.nombre_estado AS estado_reserva
                FROM tbl_reserva r
                JOIN tbl_estado er ON er.id_estado = r.id_estado_reserva
                JOIN tbl_categoria_estado c ON c.id_categoria_estado = er.id_categoria_estado
                WHERE c.nombre_categoria_estado = 'tbl_reserva'
                  AND er.nombre_estado = 'CREADA'
                ORDER BY r.id_reserva
                """);
    }

    public List<Map<String, Object>> findCustomerReservationAvailabilityServiceProviderState() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT c.id_usuario AS client_id,
                       c.correo_usuario AS client_email,
                       r.id_reserva,
                       ds.id_disponibilidad_servicio,
                       ds.fecha_disponibilidad,
                       ds.hora_inicio,
                       ds.hora_fin,
                       s.id_servicio,
                       s.nombre_servicio,
                       p.id_usuario AS provider_id,
                       p.correo_usuario AS provider_email,
                       er.nombre_estado AS estado_reserva
                FROM tbl_usuario c
                JOIN tbl_reserva r ON r.id_usuario_cliente = c.id_usuario
                JOIN tbl_disponibilidad_servicio ds ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
                JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio
                JOIN tbl_usuario p ON p.id_usuario = s.id_usuario_proveedor
                JOIN tbl_estado er ON er.id_estado = r.id_estado_reserva
                ORDER BY r.id_reserva
                """);
    }

    public List<Map<String, Object>> findProviderServiceAvailabilityState() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT p.id_usuario AS provider_id,
                       p.correo_usuario AS provider_email,
                       s.id_servicio,
                       s.nombre_servicio,
                       ds.id_disponibilidad_servicio,
                       ds.fecha_disponibilidad,
                       ds.hora_inicio,
                       ds.hora_fin,
                       ed.nombre_estado AS estado_disponibilidad
                FROM tbl_usuario p
                JOIN tbl_rol r ON r.id_rol = p.id_rol
                JOIN tbl_servicio s ON s.id_usuario_proveedor = p.id_usuario
                LEFT JOIN tbl_disponibilidad_servicio ds ON ds.id_servicio = s.id_servicio
                LEFT JOIN tbl_estado ed ON ed.id_estado = ds.id_estado_disponibilidad
                WHERE r.nombre_rol = 'PROVEEDOR'
                ORDER BY p.id_usuario, s.id_servicio, ds.fecha_disponibilidad NULLS LAST, ds.hora_inicio NULLS LAST
                """);
    }
}