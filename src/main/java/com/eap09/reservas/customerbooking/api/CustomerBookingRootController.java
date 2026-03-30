package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PROTECTED + "/customer-booking")
public class CustomerBookingRootController {

    @GetMapping("/bootstrap")
    public ApiResponse<String> bootstrap() {
        return new ApiResponse<>(
                "Customer booking bootstrap ready",
                "Use this module to implement offer query, slot query and reservation use cases",
                TraceIdUtil.currentTraceId());
    }
}
