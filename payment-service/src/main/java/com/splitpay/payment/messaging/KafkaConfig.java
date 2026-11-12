package com.splitpay.payment.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    NewTopic ordersPlacedTopic(@Value("${app.topics.orders-placed}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }

    /** Dead-letter topic: messages that still fail after all retries end up here for investigation. */
    @Bean
    NewTopic ordersPlacedDltTopic(@Value("${app.topics.orders-placed}") String name) {
        return TopicBuilder.name(name + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentResultsTopic(@Value("${app.topics.payment-results}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }

    /**
     * Retries a failed message a few times with a pause between attempts, then publishes it to
     * "<topic>.DLT" so it doesn't block the messages behind it. Spring Boot applies this
     * error handler to the @KafkaListener automatically.
     */
    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                     @Value("${app.retry.attempts}") long attempts,
                                     @Value("${app.retry.interval-ms}") long intervalMs) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(intervalMs, attempts));
        // A message that can't be read will never succeed, so send it straight to the DLT
        handler.addNotRetryableExceptions(DeserializationException.class);
        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Attempt {} failed for message with key {}: {}", attempt, record.key(), ex.getMessage()));
        return handler;
    }
}
