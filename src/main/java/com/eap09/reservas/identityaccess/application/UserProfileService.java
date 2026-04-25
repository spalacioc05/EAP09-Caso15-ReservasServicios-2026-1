package com.eap09.reservas.identityaccess.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.common.exception.ProfileNoChangesException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileRequest;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileResponse;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final String PROFILE_EVENT_TYPE = "ACTUALIZACION_PERFIL_USUARIO";
    private static final String USER_ENTITY_TYPE = "tbl_usuario";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserAccountRepository userAccountRepository;
    private final SystemEventPublisher systemEventPublisher;

    public UserProfileService(UserAccountRepository userAccountRepository,
                              SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public UpdateOwnProfileResponse updateOwnProfile(String authenticatedUsername,
                                                     UpdateOwnProfileRequest request) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "No se encontro el perfil autenticado"));

        try {
            String nombresNormalizados = normalize(request.nombres());
            String apellidosNormalizados = normalize(request.apellidos());
            String correoNormalizado = normalizeEmail(request.correo());

            validatePatchPayload(nombresNormalizados, apellidosNormalizados, correoNormalizado);

            if (correoNormalizado != null
                    && !correoNormalizado.equalsIgnoreCase(user.getCorreoUsuario())
                    && userAccountRepository.existsByCorreoUsuarioIgnoreCaseAndIdUsuarioNot(correoNormalizado, user.getIdUsuario())) {
                throw new EmailAlreadyRegisteredException("El correo ingresado ya esta registrado");
            }

            String nombresFinales = nombresNormalizados == null ? user.getNombresUsuario() : nombresNormalizados;
            String apellidosFinales = apellidosNormalizados == null ? user.getApellidosUsuario() : apellidosNormalizados;
            String correoFinal = correoNormalizado == null ? user.getCorreoUsuario() : correoNormalizado;

            if (nombresFinales.equals(user.getNombresUsuario())
                    && apellidosFinales.equals(user.getApellidosUsuario())
                    && correoFinal.equalsIgnoreCase(user.getCorreoUsuario())) {
                throw new ProfileNoChangesException("No existen cambios para aplicar");
            }

            user.setNombresUsuario(nombresFinales);
            user.setApellidosUsuario(apellidosFinales);
            user.setCorreoUsuario(correoFinal);

            UserAccountEntity updatedUser = userAccountRepository.save(user);
            publishResultEvent(updatedUser.getIdUsuario(), "EXITO", "Perfil propio actualizado correctamente");

            return new UpdateOwnProfileResponse(
                    updatedUser.getIdUsuario(),
                    updatedUser.getNombresUsuario(),
                    updatedUser.getApellidosUsuario(),
                    updatedUser.getCorreoUsuario());
        } catch (RuntimeException ex) {
            publishResultEvent(user.getIdUsuario(), "FALLO", resolveFailureDetail(ex));
            throw ex;
        }
    }

    private void validatePatchPayload(String nombres,
                                      String apellidos,
                                      String correo) {
        if (nombres == null && apellidos == null && correo == null) {
            throw new ApiException("PROFILE_UPDATE_PAYLOAD_EMPTY", "Debe enviar al menos un campo para actualizar");
        }

        if (nombres != null && nombres.isEmpty()) {
            throw new ApiException("PROFILE_NAME_REQUIRED", "nombres no puede estar vacio");
        }

        if (apellidos != null && apellidos.isEmpty()) {
            throw new ApiException("PROFILE_LASTNAME_REQUIRED", "apellidos no puede estar vacio");
        }

        if (correo != null) {
            if (correo.isEmpty()) {
                throw new ApiException("PROFILE_EMAIL_REQUIRED", "correo no puede estar vacio");
            }
            if (!EMAIL_PATTERN.matcher(correo).matches()) {
                throw new ApiException("PROFILE_EMAIL_INVALID", "El correo ingresado no es valido");
            }
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String resolveFailureDetail(RuntimeException ex) {
        if (ex instanceof ApiException
                || ex instanceof EmailAlreadyRegisteredException
                || ex instanceof ProfileNoChangesException) {
            return ex.getMessage();
        }
        return "No fue posible completar la actualizacion del perfil";
    }

    private void publishResultEvent(Long userId, String result, String details) {
        List<String> normalizedDetails = new ArrayList<>();
        if (details != null && !details.isBlank()) {
            normalizedDetails.add(details.trim());
        }

        systemEventPublisher.publish(SystemEvent.now(
                PROFILE_EVENT_TYPE,
                USER_ENTITY_TYPE,
                String.valueOf(userId),
                String.valueOf(userId),
                result,
                String.join("; ", normalizedDetails),
                TraceIdUtil.currentTraceId()));
    }
}