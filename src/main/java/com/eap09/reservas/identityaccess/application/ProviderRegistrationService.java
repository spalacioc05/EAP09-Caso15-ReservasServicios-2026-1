package com.eap09.reservas.identityaccess.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationRequest;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationResponse;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderRegistrationService {

    private static final String PROVIDER_ROLE = "PROVEEDOR";
    private static final String USER_STATE_CATEGORY = "tbl_usuario";
    private static final String ACTIVE_STATE = "ACTIVA";

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final StateRepository stateRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemEventPublisher systemEventPublisher;

    public ProviderRegistrationService(UserAccountRepository userAccountRepository,
                                       RoleRepository roleRepository,
                                       StateRepository stateRepository,
                                       PasswordEncoder passwordEncoder,
                                       SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.stateRepository = stateRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public ProviderRegistrationResponse registerProvider(ProviderRegistrationRequest request) {
        String normalizedEmail = request.correo().trim().toLowerCase();
        if (userAccountRepository.existsByCorreoUsuarioIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException("El correo ingresado ya esta registrado");
        }

        RoleEntity providerRole = roleRepository.findByNombreRol(PROVIDER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Required role PROVEEDOR was not found"));

        StateEntity activeUserState = stateRepository.findByCategoryAndStateName(USER_STATE_CATEGORY, ACTIVE_STATE)
                .orElseThrow(() -> new IllegalStateException("Required state ACTIVA for tbl_usuario was not found"));

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario(request.nombres().trim());
        user.setApellidosUsuario(request.apellidos().trim());
        user.setCorreoUsuario(normalizedEmail);
        user.setHashContrasenaUsuario(passwordEncoder.encode(request.contrasena()));
        user.setRol(providerRole);
        user.setIdEstado(activeUserState.getIdEstado());
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        UserAccountEntity createdUser = userAccountRepository.save(user);

        systemEventPublisher.publish(SystemEvent.now(
                "REGISTRO_PROVEEDOR",
                "tbl_usuario",
                String.valueOf(createdUser.getIdUsuario()),
                "EXITO",
                "Cuenta de proveedor creada",
                TraceIdUtil.currentTraceId()));

        return new ProviderRegistrationResponse(
                createdUser.getIdUsuario(),
                createdUser.getCorreoUsuario(),
                providerRole.getNombreRol(),
                ACTIVE_STATE
        );
    }
}
