package com.eap09.reservas.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSystemEventPublisher implements SystemEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSystemEventPublisher.class);

    @Override
    public void publish(SystemEvent event) {
        LOGGER.info("system_event type={} entityType={} entityId={} result={} traceId={} details={}",
                event.type(),
                event.entityType(),
                event.entityId(),
                event.result(),
                event.traceId(),
                event.details());
    }
}
