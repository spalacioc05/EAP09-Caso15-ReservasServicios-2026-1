package com.eap09.reservas.identityaccess.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProviderRegistrationE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanupTestData() {
        for (String email : createdEmails) {
            jdbcTemplate.update("""
                    DELETE FROM tbl_evento
                    WHERE id_usuario_responsable IN (
                        SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?
                    )
                       OR id_registro_afectado IN (
                        SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?
                    )
                    """, email, email);
            jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario IN (SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?)", email);
            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }
        createdEmails.clear();
    }

    @Test
    void shouldRegisterProviderEndToEndAndPersistRoleStateHashAndEvent() throws Exception {
        String email = uniqueEmail();
        createdEmails.add(email);

        ResponseEntity<String> response = postRegistration(email, "Password1!");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());

        assertEquals("Proveedor registrado correctamente", body.path("message").asText());
        assertEquals(email, body.path("data").path("correo").asText());
        assertEquals("PROVEEDOR", body.path("data").path("rol").asText());
        assertEquals("ACTIVA", body.path("data").path("estado").asText());
        assertTrue(body.hasNonNull("traceId"));

        UserAccountEntity saved = userAccountRepository.findByCorreoUsuarioIgnoreCase(email).orElseThrow();
        assertEquals("PROVEEDOR", saved.getRol().getNombreRol());

        String estadoNombre = jdbcTemplate.queryForObject("""
                SELECT e.nombre_estado
                FROM tbl_estado e
                WHERE e.id_estado = ?
                """, String.class, saved.getIdEstado());
        assertEquals("ACTIVA", estadoNombre);

        assertNotEquals("Password1!", saved.getHashContrasenaUsuario());
        assertTrue(new BCryptPasswordEncoder().matches("Password1!", saved.getHashContrasenaUsuario()));

        Integer eventCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'REGISTRO_PROVEEDOR'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, saved.getIdUsuario());
        assertNotNull(eventCount);
        assertTrue(eventCount > 0);
    }

    @Test
    void shouldRejectDuplicateEmailEndToEnd() throws Exception {
        String email = uniqueEmail();
        createdEmails.add(email);

        ResponseEntity<String> first = postRegistration(email, "Password1!");
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        ResponseEntity<String> second = postRegistration(email, "Password1!");
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());

        JsonNode body = objectMapper.readTree(second.getBody());
        assertEquals("EMAIL_ALREADY_REGISTERED", body.path("errorCode").asText());
        assertEquals("El correo ingresado ya esta registrado", body.path("message").asText());
        assertTrue(body.path("details").isArray());
    }

    @Test
    void shouldRejectInvalidPayloadEndToEnd() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = testRestTemplate.postForEntity(
                "/api/v1/providers",
                new HttpEntity<>("""
                        {
                          "nombres":"",
                          "apellidos":"",
                          "correo":"",
                          "contrasena":""
                        }
                        """, headers),
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
        assertEquals("Validacion de la solicitud fallida", body.path("message").asText());
    }

    @Test
    void shouldRejectWeakPasswordEndToEnd() throws Exception {
        String email = uniqueEmail();

        ResponseEntity<String> response = postRegistration(email, "weak");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectInvalidEmailEndToEnd() throws Exception {
        ResponseEntity<String> response = postRegistration("correo-invalido", "Password1!");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    private ResponseEntity<String> postRegistration(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
                "nombres", "Proveedor",
                "apellidos", "Prueba",
                "correo", email,
                "contrasena", password
        );

        return testRestTemplate.postForEntity(
                "/api/v1/providers",
                new HttpEntity<>(request, headers),
                String.class);
    }

    private String uniqueEmail() {
        return "hu02.provider." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
    }
}
