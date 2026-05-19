package com.eap09.reservas.integrationtests.provideroffer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.springframework.test.web.servlet.MockMvc;

import com.eap09.reservas.ReservasApplication;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import com.eap09.reservas.provideroffer.infrastructure.GeneralScheduleRepository;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
                classes = ReservasApplication.class)

@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class GeneralScheduleIntegrationTest {

    private UserAccountEntity provider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GeneralScheduleRepository generalScheduleRepository;

    @BeforeEach
    void setUp() {
        cleanup();
        insertTestData();
    }

    void insertTestData(){

        // Create the provider
        provider = new UserAccountEntity();
        provider.setNombresUsuario("Proveedor");
        provider.setApellidosUsuario("Lopez");
        provider.setCorreoUsuario("proveedor@example.com");
        provider.setHashContrasenaUsuario("hash:#ProviderPassword1!");
        provider.setIdEstado(1L);
        provider.setIntentosFallidosConsecutivos(0);

        RoleEntity userRole = new RoleEntity();
        userRole.setIdRol(2L);
        userRole.setNombreRol("PROVEEDOR");
        provider.setRol(userRole);

        UserAccountEntity savedUser = userAccountRepository.save(provider);
        provider.setIdUsuario(savedUser.getIdUsuario());

    }
        
    void cleanup() {
        String truncate_sql = """
            TRUNCATE TABLE tbl_evento, tbl_horario_general_proveedor,
            tbl_usuario RESTART IDENTITY CASCADE
        """;
        jdbcTemplate.update(truncate_sql);
    }

    void defineSchedule(){

        // Create existing schedule for update later
        WeekDayEntity day = new WeekDayEntity();
        day.setIdDiaSemana(1L);
        day.setNombreDiaSemana("LUNES");
        day.setOrdenDiaSemana(1);

        OffsetDateTime now = OffsetDateTime.now();

        GeneralScheduleEntity entity;
        entity = new GeneralScheduleEntity();
        entity.setIdUsuarioProveedor(provider.getIdUsuario());
        entity.setDiaSemana(day);
        entity.setFechaCreacionHorarioGeneral(now);

        entity.setHoraInicio(java.time.LocalTime.of(7,0));
        entity.setHoraFin(java.time.LocalTime.of(18,0));
        entity.setFechaActualizacionHorarioGeneral(now);

        generalScheduleRepository.save(entity);
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should define sucessfully the provider general schedule")
    void shouldDefineGeneralScheduleSuccessfully() throws Exception{

        Long id = provider.getIdUsuario();

        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                                                                                                .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"08:00:00",
                                  "horaFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Horario general definido correctamente"))
                .andExpect(jsonPath("$.data.providerUserId").value(id))
                .andExpect(jsonPath("$.data.dayOfWeek").value("LUNES"));
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should update sucessfully an existing general schedule")
    void shouldUpdateGeneralScheduleSuccessfully() throws Exception{

        Long id = provider.getIdUsuario();
        defineSchedule();

        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                                                                                                .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"08:00:00",
                                  "horaFin":"15:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Horario general definido correctamente"))
                .andExpect(jsonPath("$.data.providerUserId").value(id))
                .andExpect(jsonPath("$.data.dayOfWeek").value("LUNES"))
                .andExpect(jsonPath("$.data.horaInicio").value("08:00:00"))
                .andExpect(jsonPath("$.data.horaFin").value("15:00:00"));
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should throws invalid time range Exception when not valid time range")
    void shouldReturnInvalidTimeRangeError() throws Exception{

        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                                                                                                .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"18:00:00",
                                  "horaFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La hora de fin debe ser posterior a la hora de inicio"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_TIME_RANGE"));
    }


}
