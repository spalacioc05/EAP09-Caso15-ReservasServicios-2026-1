package com.eap09.reservas.common.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PROTECTED)
public class ProtectedBaseController {

    @GetMapping("/status")
    public ApiResponse<String> status(Authentication authentication) {
        return new ApiResponse<>(
                "Protected endpoint available",
                "Authenticated as: " + authentication.getName(),
                TraceIdUtil.currentTraceId());
    }
}
