package com.eap09.reservas;

import com.eap09.reservas.security.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(SecurityProperties.class)
public class ReservasApplication {


    @PostConstruct
    public void init() {
        // Forzamos zona horaria global a UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ReservasApplication.class, args);
    }
}
