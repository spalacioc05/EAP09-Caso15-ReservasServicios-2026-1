package com.eap09.reservas.identityaccess.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH)
public class IdentityAccessRootController {

    @GetMapping("/bootstrap")
    public ApiResponse<String> bootstrap() {
        return new ApiResponse<>(
                "Identity and access bootstrap ready",
                "Use this module to implement registration and authentication use cases",
                TraceIdUtil.currentTraceId());
    }
}
