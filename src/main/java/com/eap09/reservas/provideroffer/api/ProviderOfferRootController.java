package com.eap09.reservas.provideroffer.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PROTECTED + "/provider-offer")
public class ProviderOfferRootController {

    @GetMapping("/bootstrap")
    public ApiResponse<String> bootstrap() {
        return new ApiResponse<>(
                "Provider offer bootstrap ready",
                "Use this module to implement schedule, service and availability use cases",
                TraceIdUtil.currentTraceId());
    }
}
