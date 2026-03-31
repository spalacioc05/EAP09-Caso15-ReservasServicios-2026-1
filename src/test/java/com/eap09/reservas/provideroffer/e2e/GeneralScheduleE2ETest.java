package com.eap09.reservas.provideroffer.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import com.eap09.reservas.provideroffer.infrastructure.GeneralScheduleRepository;
import com.eap09.reservas.provideroffer.infrastructure.WeekDayRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeneralScheduleE2ETest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GeneralScheduleRepository generalScheduleRepository;

    @Autowired
    private WeekDayRepository weekDayRepository;

    private final List<String> createdEmails = new ArrayList<>();
    private Long activeStateId;

    @BeforeEach
    void setup() {
        activeStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();
    }

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            Long userId = jdbcTemplate.query("SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?", rs ->
                    rs.next() ? rs.getLong(1) : null, email);

            if (userId != null) {
                jdbcTemplate.update("DELETE FROM tbl_horario_general_proveedor WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_evento WHERE id_usuario_responsable = ? OR id_registro_afectado = ?", userId, userId);
            }

            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }

        createdEmails.clear();
    }

    @Test
    void shouldDefineGeneralScheduleSuccessfully() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = putSchedule(token, "LUNES", "08:00:00", "12:00:00");
        assertEquals(200, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Horario general definido correctamente", body.path("message").asText());
        assertEquals("LUNES", body.path("data").path("dayOfWeek").asText());

        Long userId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail).orElseThrow().getIdUsuario();
        Integer scheduleCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_horario_general_proveedor h
                JOIN tbl_dia_semana d ON d.id_dia_semana = h.id_dia_semana
                WHERE h.id_usuario_proveedor = ?
                  AND d.nombre_dia_semana = 'LUNES'
                """, Integer.class, userId);
        assertEquals(1, scheduleCount);
    }

    @Test
    void shouldReplaceExistingScheduleForSameDayKeepingSingleRange() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult first = putSchedule(token, "MARTES", "08:00:00", "12:00:00");
        assertEquals(200, first.statusCode(), first.body());

        HttpResult second = putSchedule(token, "MARTES", "09:00:00", "13:00:00");
        assertEquals(200, second.statusCode(), second.body());

        JsonNode secondBody = objectMapper.readTree(second.body());
        LocalTime expectedStart = LocalTime.parse(secondBody.path("data").path("horaInicio").asText());
        LocalTime expectedEnd = LocalTime.parse(secondBody.path("data").path("horaFin").asText());

        Long userId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail).orElseThrow().getIdUsuario();
        WeekDayEntity martes = weekDayRepository.findByNombreDiaSemana("MARTES").orElseThrow();
        List<GeneralScheduleEntity> schedules = generalScheduleRepository
            .findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(userId, martes.getIdDiaSemana());

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_horario_general_proveedor h
                JOIN tbl_dia_semana d ON d.id_dia_semana = h.id_dia_semana
                WHERE h.id_usuario_proveedor = ?
                  AND d.nombre_dia_semana = 'MARTES'
                """, Integer.class, userId);

        assertEquals(1, count);
        assertEquals(1, schedules.size());
        assertEquals(expectedStart, schedules.get(0).getHoraInicio());
        assertEquals(expectedEnd, schedules.get(0).getHoraFin());
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = putScheduleRaw(token, "MIERCOLES", """
                {
                  "horaInicio":null,
                  "horaFin":null
                }
                """);

        assertEquals(400, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenEndTimeIsBeforeStartTime() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = putSchedule(token, "JUEVES", "15:00:00", "09:00:00");
        assertEquals(400, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("INVALID_TIME_RANGE", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotProvider() throws Exception {
        String clientEmail = createUser("CLIENTE", "Password1!");
        String token = authenticate(clientEmail, "Password1!");

        HttpResult response = putSchedule(token, "VIERNES", "08:00:00", "12:00:00");
        assertEquals(403, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("PROVIDER_ROLE_REQUIRED", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenMissingAuthentication() {
        HttpResult response = putSchedule(null, "SABADO", "08:00:00", "12:00:00");
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    @Test
    void shouldRegisterDefinitionEvent() {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = putSchedule(token, "DOMINGO", "10:00:00", "14:00:00");
        assertEquals(200, response.statusCode(), response.body());

        Long userId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail).orElseThrow().getIdUsuario();
        Integer eventCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'DEFINICION_HORARIO_GENERAL'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, userId);

        assertNotNull(eventCount);
        assertTrue(eventCount > 0);
    }

    private HttpResult putSchedule(String token, String dayOfWeek, String startTime, String endTime) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "horaInicio", startTime,
                    "horaFin", endTime
            ));
            return putScheduleRaw(token, dayOfWeek, body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult putScheduleRaw(String token, String dayOfWeek, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/general-schedule/" + dayOfWeek))
                    .header("Content-Type", "application/json");

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = builder
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String authenticate(String email, String rawPassword) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "correo", email,
                    "contrasena", rawPassword
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/auth/sessions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("No se pudo autenticar usuario de prueba: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("accessToken").asText();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String createUser(String roleName, String rawPassword) {
        RoleEntity role = roleRepository.findByNombreRol(roleName).orElseThrow();

        String email = "hu08." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Horario");
        user.setApellidosUsuario("General");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(activeStateId);
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        userAccountRepository.save(user);
        return email;
    }

    private record HttpResult(int statusCode, String body) {
    }
}
