package com.eap09.reservas.security.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String roleName;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public SecurityUserPrincipal(Long userId,
                                 String username,
                                 String password,
                                 String roleName,
                                 boolean enabled,
                                 boolean accountNonLocked) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.roleName = roleName;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
