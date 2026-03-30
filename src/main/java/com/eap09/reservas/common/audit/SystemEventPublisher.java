package com.eap09.reservas.common.audit;

public interface SystemEventPublisher {

    void publish(SystemEvent event);
}
