package com.eap09.reservas.config;

import java.time.Clock;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimezoneConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.timeZone(TimeZone.getTimeZone("UTC"));
    }

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}