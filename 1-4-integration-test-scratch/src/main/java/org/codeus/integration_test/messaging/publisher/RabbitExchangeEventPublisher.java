package org.codeus.integration_test.messaging.publisher;

import org.codeus.integration_test.messaging.event.ExchangeCompletedEvent;
import org.codeus.integration_test.messaging.event.ExchangeFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static org.codeus.integration_test.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitExchangeEventPublisher implements ExchangeEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishCompletedEvent(ExchangeCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);
            log.debug("Published exchange completed event for exchange ID {}", event.getExchangeId());
        } catch (AmqpException e) {
            log.error("Failed to publish completed event for exchange ID {}: {}", event.getExchangeId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void publishFailedEvent(ExchangeFailedEvent event) {
        try {
            rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY, event);
            log.debug("Published exchange failed event for exchange ID {}", event.getExchangeId());
        } catch (AmqpException e) {
            log.error("Failed to publish failed event for exchange ID {}: {}", event.getExchangeId(), e.getMessage(), e);
            throw e;
        }
    }
}
