package com.eap09.reservas.support;

import com.eap09.reservas.common.exception.CommonExceptionHandler;
import com.eap09.reservas.common.exception.CustomerBookingExceptionHandler;
import com.eap09.reservas.common.exception.IdentityAccessExceptionHandler;
import com.eap09.reservas.common.exception.ProviderOfferExceptionHandler;
import com.eap09.reservas.common.exception.ValidationExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
        ValidationExceptionHandler.class,
        IdentityAccessExceptionHandler.class,
        ProviderOfferExceptionHandler.class,
        CustomerBookingExceptionHandler.class,
        CommonExceptionHandler.class
})
public class ControllerAdviceTestConfig {
}