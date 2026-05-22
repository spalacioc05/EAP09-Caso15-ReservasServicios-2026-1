package com.eap09.reservas.provideroffer.api;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.support.ControllerAdviceTestConfig;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleResponse;
import com.eap09.reservas.provideroffer.application.GeneralScheduleService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GeneralScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ControllerAdviceTestConfig.class)
class GeneralScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeneralScheduleService generalScheduleService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

        @MockBean
        private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldDefineGeneralScheduleSuccessfully() throws Exception {
        GeneralScheduleResponse response = new GeneralScheduleResponse(
                10L,
                "LUNES",
                java.time.LocalTime.of(8, 0),
                java.time.LocalTime.of(12, 0)
        );

        when(generalScheduleService.upsertGeneralSchedule(eq("provider@test.local"), eq("LUNES"), any()))
                .thenReturn(response);

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
                .andExpect(jsonPath("$.data.providerUserId").value(10))
                .andExpect(jsonPath("$.data.dayOfWeek").value("LUNES"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                        .principal(() -> "provider@test.local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":null,
                                  "horaFin":null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validacion de la solicitud fallida"))
                .andExpect(jsonPath("$.details", hasItems(
                        "horaInicio: horaInicio es obligatoria",
                        "horaFin: horaFin es obligatoria"
                )));

        verify(generalScheduleService, never()).upsertGeneralSchedule(any(), any(), any());
    }

    @Test
    void shouldRejectBlankHoraInicioWithoutInvokingService() throws Exception {
        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                        .principal(() -> "provider@test.local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"",
                                  "horaFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validacion de la solicitud fallida"))
                .andExpect(jsonPath("$.details", hasItems(
                        "horaInicio: horaInicio es obligatoria"
                )));

        verify(generalScheduleService, never()).upsertGeneralSchedule(any(), any(), any());
    }

    @Test
    void shouldRejectBlankHoraFinWithoutInvokingService() throws Exception {
        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                        .principal(() -> "provider@test.local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"08:00:00",
                                  "horaFin":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validacion de la solicitud fallida"))
                .andExpect(jsonPath("$.details", hasItems(
                        "horaFin: horaFin es obligatoria"
                )));

        verify(generalScheduleService, never()).upsertGeneralSchedule(any(), any(), any());
    }

    @Test
    void shouldRejectBlankHoraInicioAndHoraFinWithoutInvokingService() throws Exception {
        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                        .principal(() -> "provider@test.local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"",
                                  "horaFin":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validacion de la solicitud fallida"))
                .andExpect(jsonPath("$.details").isArray());

        verify(generalScheduleService, never()).upsertGeneralSchedule(any(), any(), any());
    }

    @Test
    void shouldRejectInvalidDayOfWeekWithControlledError() throws Exception {
        when(generalScheduleService.upsertGeneralSchedule(eq("provider@test.local"), eq("INVALID_DAY"), any()))
                .thenThrow(new ApiException("INVALID_DAY_OF_WEEK", "El dia de la semana ingresado no es valido"));

        mockMvc.perform(put("/api/v1/providers/me/general-schedule/INVALID_DAY")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"08:00:00",
                                  "horaFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_DAY_OF_WEEK"))
                .andExpect(jsonPath("$.message").value("El dia de la semana ingresado no es valido"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotProvider() throws Exception {
        when(generalScheduleService.upsertGeneralSchedule(eq("cliente@test.local"), eq("LUNES"), any()))
                .thenThrow(new ProviderRoleRequiredException("Solo un proveedor autenticado puede definir el horario general"));

        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                                                                                                .principal(new UsernamePasswordAuthenticationToken("cliente@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"08:00:00",
                                  "horaFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"08:00:00",
                                  "horaFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnInternalErrorWhenUnexpectedFailure() throws Exception {
        when(generalScheduleService.upsertGeneralSchedule(eq("provider@test.local"), eq("LUNES"), any()))
                .thenThrow(new RuntimeException("db unavailable"));

        mockMvc.perform(put("/api/v1/providers/me/general-schedule/LUNES")
                                                                                                .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "horaInicio":"08:00:00",
                                  "horaFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }
}
