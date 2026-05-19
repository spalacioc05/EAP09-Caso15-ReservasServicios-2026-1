package com.eap09.reservas.integrationtests.provideroffer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.springframework.test.web.servlet.MockMvc;

import com.eap09.reservas.ReservasApplication;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.*;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
                classes = ReservasApplication.class)

@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class ServiceRegistrationIntegrationTest {

    private UserAccountEntity provider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ServiceRepository serviceRepository;


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
            TRUNCATE TABLE tbl_evento, tbl_servicio,
            tbl_usuario RESTART IDENTITY CASCADE;
        """;
        jdbcTemplate.update(truncate_sql);
    }

    void defineService(){

        OffsetDateTime now = OffsetDateTime.now();

        ServiceEntity entity = new ServiceEntity();
        entity.setIdUsuarioProveedor(provider.getIdUsuario());
        entity.setIdEstadoServicio(3L); 
        entity.setNombreServicio("masaje terapeutico");
        entity.setDescripcionServicio("Sesión de relajación");
        entity.setDuracionMinutos(60);
        entity.setCapacidadMaximaConcurrente(2);
        entity.setFechaCreacionServicio(now);
        entity.setFechaActualizacionServicio(now);

        serviceRepository.save(entity);
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should register sucessfully one service for the provider")
    void shouldRegisterServiceSuccessfully() throws Exception{

        mockMvc.perform(post("/api/v1/providers/me/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "nombre":"Masaje terapeutico",
                            "descripcion":"Sesion de relajacion",
                            "duracionMinutos":60,
                            "capacidadMaximaConcurrente":2
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Servicio registrado correctamente"))
                .andExpect(jsonPath("$.data.idServicio").value(1))
                .andExpect(jsonPath("$.data.estadoServicio").value("ACTIVO"));
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should throws an empty field exception when name is empty")
    void shouldReturnEmptyNameException() throws Exception{

        mockMvc.perform(post("/api/v1/providers/me/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "nombre":"",
                            "descripcion":"Sesion de relajacion",
                            "duracionMinutos":60,
                            "capacidadMaximaConcurrente":2
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El campo nombre es obligatorio"));
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should throws an empty field exception when description is empty")
    void shouldReturnEmptyDescriptionException() throws Exception{

        mockMvc.perform(post("/api/v1/providers/me/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "nombre":"Masaje terapeutico",
                            "descripcion":"",
                            "duracionMinutos":60,
                            "capacidadMaximaConcurrente":2
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El campo descripción es obligatorio"));
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should throws an empty field exception when duration is empty")
    void shouldReturnEmptyDurationException() throws Exception{

        mockMvc.perform(post("/api/v1/providers/me/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "nombre":"Masaje terapeutico",
                            "descripcion":"Sesion de relajación",
                            "duracionMinutos":null,
                            "capacidadMaximaConcurrente":2
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El campo duración es obligatorio"));
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should throws an empty field exception when capacity is empty")
    void shouldReturnEmptyCapacityException() throws Exception{

        mockMvc.perform(post("/api/v1/providers/me/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "nombre":"Masaje terapeutico",
                            "descripcion":"Sesion de relajación",
                            "duracionMinutos":45,
                            "capacidadMaximaConcurrente":null
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El campo capacidad máxima es obligatorio"));
    }

    @Test
    @WithMockUser(username = "proveedor@example.com", roles = {"PROVEEDOR"})
    @DisplayName("Should throws service duplicated exception when service name is already registered")
    void shouldReturnDiplicatedServiceException() throws Exception{

        //Create the service
        defineService();

        mockMvc.perform(post("/api/v1/providers/me/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "nombre":"Masaje terapeutico",
                            "descripcion":"Sesion de relajación",
                            "duracionMinutos":60,
                            "capacidadMaximaConcurrente":2
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_NAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("No es posible crear un servicio con nombre repetido para el mismo proveedor"));
    }

}
