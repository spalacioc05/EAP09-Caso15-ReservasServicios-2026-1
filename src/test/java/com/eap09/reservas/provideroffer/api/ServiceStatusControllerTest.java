package com.eap09.reservas.provideroffer.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusResponse;
import com.eap09.reservas.provideroffer.application.ServiceStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller tests para ServiceStatusController.
 * Pruebas con MockMvc, sin BD real.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(ServiceStatusController.class)
@DisplayName("ServiceStatusController Tests")
class ServiceStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceStatusService serviceStatusService;

    private static final String BASE_URL = "/api/v1/providers/me/services";

    @BeforeEach
    void setUp() {
        // Setup común si es necesario
    }

    // ========== Activación Exitosa (200 OK) ==========

    @Test
    @DisplayName("200 OK: Activación exitosa con autenticación válida")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testActivateServiceSuccess() throws Exception {
        // Arrange
        Long serviceId = 1L;
        ServiceStatusResponse response = new ServiceStatusResponse(
                serviceId,
                "Consultoría de Arquitectura",
                "ACTIVO");

        when(serviceStatusService.updateServiceStatus(
                eq("proveedor@mail.com"),
                eq(serviceId),
                any(ServiceStatusRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ACTIVO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idServicio", is(1)))
                .andExpect(jsonPath("$.data.nombreServicio", is("Consultoría de Arquitectura")))
                .andExpect(jsonPath("$.data.estadoActual", is("ACTIVO")))
                .andExpect(jsonPath("$.mensaje", notNullValue()));

        verify(serviceStatusService).updateServiceStatus(
                eq("proveedor@mail.com"),
                eq(serviceId),
                any());
    }

    // ========== Inactivación Exitosa (200 OK) ==========

    @Test
    @DisplayName("200 OK: Inactivación exitosa con autenticación válida")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testDeactivateServiceSuccess() throws Exception {
        // Arrange
        Long serviceId = 1L;
        ServiceStatusResponse response = new ServiceStatusResponse(
                serviceId,
                "Consultoría de Arquitectura",
                "INACTIVO");

        when(serviceStatusService.updateServiceStatus(
                eq("proveedor@mail.com"),
                eq(serviceId),
                any(ServiceStatusRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("INACTIVO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idServicio", is(1)))
                .andExpect(jsonPath("$.data.estadoActual", is("INACTIVO")));
    }

    // ========== Sin Autenticación (401 Unauthorized) ==========

    @Test
    @DisplayName("401 Unauthorized: sin token JWT")
    void testMissingAuthenticationReturns401() throws Exception {
        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ACTIVO"))))
                .andExpect(status().isUnauthorized());
    }

    // ========== Rol Incorrecto (403 Forbidden) ==========

    @Test
    @DisplayName("403 Forbidden: usuario con rol CLIENTE intenta cambiar estado de servicio")
    @WithMockUser(username = "cliente@mail.com", roles = "CLIENTE")
    void testClientRoleReturns403() throws Exception {
        // Arrange
        Long serviceId = 1L;

        when(serviceStatusService.updateServiceStatus(
                eq("cliente@mail.com"),
                eq(serviceId),
                any()))
                .thenThrow(new ProviderRoleRequiredException("Solo un proveedor autenticado puede cambiar el estado de servicios"));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ACTIVO"))))
                .andExpect(status().isForbidden());
    }

    // ========== Servicio No Encontrado (404 Not Found) ==========

    @Test
    @DisplayName("404 Not Found: servicio no existe")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testServiceNotFoundReturns404() throws Exception {
        // Arrange
        Long serviceId = 999L;

        when(serviceStatusService.updateServiceStatus(
                eq("proveedor@mail.com"),
                eq(serviceId),
                any()))
                .thenThrow(new ResourceNotFoundException("SERVICE_NOT_FOUND", "El servicio indicado no existe"));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ACTIVO"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("SERVICE_NOT_FOUND")));
    }

    // ========== Servicio Ajeno (403 Forbidden) ==========

    @Test
    @DisplayName("403 Forbidden: servicio no pertenece al proveedor autenticado")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testServiceNotOwnedReturns403() throws Exception {
        // Arrange
        Long serviceId = 1L;

        when(serviceStatusService.updateServiceStatus(
                eq("proveedor@mail.com"),
                eq(serviceId),
                any()))
                .thenThrow(new AccessDeniedException("No tiene permisos para operar este servicio"));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ACTIVO"))))
                .andExpect(status().isForbidden());
    }

    // ========== Payload Inválido (400 Bad Request) ==========

    @Test
    @DisplayName("400 Bad Request: targetStatus nulo")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testNullTargetStatusReturns400() throws Exception {
        // Arrange - forzar payload inválido mediante JSON manualmente
        String invalidJson = "{\"targetStatus\": null}";

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ========== Estado Objetivo Inválido (400 Bad Request) ==========

    @Test
    @DisplayName("400 Bad Request: estado objetivo inválido (ELIMINADO)")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testInvalidStatusReturns400() throws Exception {
        // Arrange
        Long serviceId = 1L;

        when(serviceStatusService.updateServiceStatus(
                eq("proveedor@mail.com"),
                eq(serviceId),
                any()))
                .thenThrow(new com.eap09.reservas.common.exception.ApiException(
                        "INVALID_STATUS",
                        "El estado debe ser ACTIVO o INACTIVO"));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ELIMINADO"))))
                .andExpect(status().isBadRequest());
    }

    // ========== Response HATEOAS ==========

    @Test
    @DisplayName("Response contiene links HATEOAS correctos")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testResponseContainsHateoasLinks() throws Exception {
        // Arrange
        Long serviceId = 1L;
        ServiceStatusResponse response = new ServiceStatusResponse(
                serviceId,
                "Consultoría",
                "ACTIVO");

        when(serviceStatusService.updateServiceStatus(any(), any(), any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ACTIVO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", notNullValue()))
                .andExpect(jsonPath("$._links['services-list'].href", notNullValue()));
    }

    // ========== Case Insensitive en Estado ==========

    @Test
    @DisplayName("200 OK: estado activo en minúsculas (activo) se acepta")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testLowercaseStatusIsAccepted() throws Exception {
        // Arrange
        Long serviceId = 1L;
        ServiceStatusResponse response = new ServiceStatusResponse(serviceId, "Consultoría", "ACTIVO");

        when(serviceStatusService.updateServiceStatus(
                eq("proveedor@mail.com"),
                eq(serviceId),
                any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("activo"))))
                .andExpect(status().isOk());

        verify(serviceStatusService).updateServiceStatus(any(), any(), any());
    }

    // ========== Estructura de Respuesta ==========

    @Test
    @DisplayName("Response tiene estructura ApiResponse correcta")
    @WithMockUser(username = "proveedor@mail.com", roles = "PROVEEDOR")
    void testResponseStructureIsCorrect() throws Exception {
        // Arrange
        Long serviceId = 1L;
        ServiceStatusResponse response = new ServiceStatusResponse(
                serviceId,
                "Consultoría",
                "ACTIVO");

        when(serviceStatusService.updateServiceStatus(any(), any(), any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + serviceId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ServiceStatusRequest("ACTIVO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", notNullValue()))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }
}
