package com.eap09.reservas.security.application;

import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.security.config.SecurityProperties;
import com.eap09.reservas.security.domain.SecurityUserPrincipal;
import java.time.LocalDateTime;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final SecurityProperties securityProperties;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository,
                                    SecurityProperties securityProperties) {
        this.userAccountRepository = userAccountRepository;
        this.securityProperties = securityProperties;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean isActive = user.getIdEstado().equals(securityProperties.getUserActiveStateId());
        boolean notTemporarilyRestricted = user.getFechaFinRestriccionAcceso() == null
                || user.getFechaFinRestriccionAcceso().isBefore(LocalDateTime.now());

        return new SecurityUserPrincipal(
                user.getIdUsuario(),
                user.getCorreoUsuario(),
                user.getHashContrasenaUsuario(),
                user.getRol().getNombreRol(),
                isActive,
                notTemporarilyRestricted
        );
    }
}
