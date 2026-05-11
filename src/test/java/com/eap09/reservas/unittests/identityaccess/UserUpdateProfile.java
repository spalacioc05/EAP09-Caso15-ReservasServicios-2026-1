package com.eap09.reservas.unittests.identityaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileRequest;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileResponse;
import com.eap09.reservas.identityaccess.application.UserProfileService;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;

public class UserUpdateProfile {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private UserProfileService userProfileService;

    private StateEntity userActiveState;
    private RoleEntity role;
    private UserAccountEntity user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userActiveState = new StateEntity();
        userActiveState.setIdEstado(1L);
        userActiveState.setNombreEstado("ACTIVA");

        role = new RoleEntity();
        role.setIdRol(1L);
        role.setNombreRol("CLIENTE");

        user = new UserAccountEntity();
        user.setIdUsuario(1L);
        user.setCorreoUsuario("salome@gmail.com");
        user.setNombresUsuario("Salome");
        user.setApellidosUsuario("Soto");
        user.setHashContrasenaUsuario("$2a$hash");
        user.setRol(role);
        user.setIdEstado(1L);
        user.setIntentosFallidosConsecutivos(0);
    }

    @Test
    void shouldUpdateOwnProfileSuccessfully() {

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("salome@gmail.com"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenReturn(user);

        UpdateOwnProfileResponse response = userProfileService.updateOwnProfile(
                "salome@gmail.com",
                new UpdateOwnProfileRequest("Camila", "Giraldo", "salome@gmail.com"));

        assertEquals(1L, response.idUsuario());
        assertEquals("Camila", response.nombres());
        assertEquals("Giraldo", response.apellidos());
        assertEquals("salome@gmail.com", response.correo());

        verify(userAccountRepository).save(any(UserAccountEntity.class));
    }

    @Test
    void UpdateOwnProfileWithNullFields_Exception() {

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("salome@gmail.com"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenReturn(user);

        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest(
                "Camila",
                "Giraldo",
                "");

        assertThrows(UnsupportedOperationException.class,
                () -> userProfileService.updateOwnProfile("salome@gmail.com", request));
    }

    @Test
    void RejectEmailAlreadyUsedByAnotherAccount_Exception() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("salome@gmail.com"))
                .thenReturn(java.util.Optional.of(user));
        when(userAccountRepository.existsByCorreoUsuarioIgnoreCaseAndIdUsuarioNot("camilagiraldo@gmail.com", 1L))
                .thenReturn(true);

        EmailAlreadyRegisteredException ex = assertThrows(EmailAlreadyRegisteredException.class,
                () -> userProfileService.updateOwnProfile(
                        "salome@gmail.com",
                        new UpdateOwnProfileRequest("Salome", "Soto", "camilagiraldo@gmail.com")));

        assertEquals("El correo ingresado ya esta registrado", ex.getMessage());

    }
}
