package com.eap09.reservas.security.infrastructure;

import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SessionTokenValidationService sessionTokenValidationService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   SessionTokenValidationService sessionTokenValidationService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.sessionTokenValidationService = sessionTokenValidationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            boolean tokenValid = jwtService.isTokenValid(jwt, userDetails);
            boolean sessionActive = sessionTokenValidationService.isActiveSessionToken(jwtService.extractTokenId(jwt));
            boolean allowForLogoutEndpoint = isCurrentSessionLogoutRequest(request);

            if (tokenValid
                && (sessionActive || allowForLogoutEndpoint)
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked()) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isCurrentSessionLogoutRequest(HttpServletRequest request) {
        return "DELETE".equalsIgnoreCase(request.getMethod())
                && ApiPaths.AUTH.concat("/sessions/current").equals(request.getServletPath());
    }
}
