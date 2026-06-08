package org.codeus.integration_test.messaging.publisher;

import org.codeus.integration_test.messaging.event.ExchangeCompletedEvent;
import org.codeus.integration_test.messaging.event.ExchangeFailedEvent;

public interface ExchangeEventPublisher {

    void publishCompletedEvent(ExchangeCompletedEvent event);

    void publishFailedEvent(ExchangeFailedEvent event);
}
