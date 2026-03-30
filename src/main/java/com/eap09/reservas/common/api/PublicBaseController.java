package com.eap09.reservas.common.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PUBLIC)
public class PublicBaseController {

    @GetMapping("/status")
    public EntityModel<ApiResponse<String>> status() {
        ApiResponse<String> response = new ApiResponse<>(
                "Public endpoint available",
                "UP",
                TraceIdUtil.currentTraceId());

        return EntityModel.of(
                response,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(PublicBaseController.class).status()).withSelfRel());
    }
}
