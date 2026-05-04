package com.eap09.reservas.identityaccess.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.api.dto.CustomerRegistrationRequest;
import com.eap09.reservas.identityaccess.api.dto.CustomerRegistrationResponse;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRegistrationService {

    private static final String CUSTOMER_ROLE = "CLIENTE";
    private static final String USER_STATE_CATEGORY = "tbl_usuario";
    private static final String ACTIVE_STATE = "ACTIVA";

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final StateRepository stateRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemEventPublisher systemEventPublisher;

    public CustomerRegistrationService(UserAccountRepository userAccountRepository,
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
    public CustomerRegistrationResponse registerCustomer(CustomerRegistrationRequest request) {
        String normalizedEmail = request.correo().trim().toLowerCase();
        if (userAccountRepository.existsByCorreoUsuarioIgnoreCase(normalizedEmail)) {
                        throw new EmailAlreadyRegisteredException("El correo ingresado ya esta registrado");
        }

        RoleEntity customerRole = roleRepository.findByNombreRol(CUSTOMER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Required role CLIENTE was not found"));

        StateEntity activeUserState = stateRepository.findByCategoryAndStateName(USER_STATE_CATEGORY, ACTIVE_STATE)
                .orElseThrow(() -> new IllegalStateException("Required state ACTIVA for " + USER_STATE_CATEGORY + " was not found"));

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario(request.nombres().trim());
        user.setApellidosUsuario(request.apellidos().trim());
        user.setCorreoUsuario(normalizedEmail);
        user.setHashContrasenaUsuario(passwordEncoder.encode(request.contrasena()));
        user.setRol(customerRole);
        user.setIdEstado(activeUserState.getIdEstado());
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        UserAccountEntity createdUser = userAccountRepository.save(user);

                publishRegistrationEventAfterCommit(createdUser.getIdUsuario());

        return new CustomerRegistrationResponse(
                createdUser.getIdUsuario(),
                createdUser.getCorreoUsuario(),
                customerRole.getNombreRol(),
                ACTIVE_STATE
        );
    }

        private void publishRegistrationEventAfterCommit(Long userId) {
                if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                        systemEventPublisher.publish(SystemEvent.now(
                                        "REGISTRO_CLIENTE",
                                        USER_STATE_CATEGORY,
                                        String.valueOf(userId),
                                        "EXITO",
                                        "Cuenta de cliente creada",
                                        TraceIdUtil.currentTraceId()));
                        return;
                }

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                systemEventPublisher.publish(SystemEvent.now(
                                                "REGISTRO_CLIENTE",
                                                USER_STATE_CATEGORY,
                                                String.valueOf(userId),
                                                "EXITO",
                                                "Cuenta de cliente creada",
                                                TraceIdUtil.currentTraceId()));
                        }
                });
        }
}
