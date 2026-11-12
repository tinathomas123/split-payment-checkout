package com.splitpay.order.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Creates the topics on startup if they don't exist yet (Spring Boot's KafkaAdmin does the work). */
@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic ordersPlacedTopic(@Value("${app.topics.orders-placed}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentResultsTopic(@Value("${app.topics.payment-results}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
