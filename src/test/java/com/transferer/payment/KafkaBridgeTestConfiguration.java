package com.transferer.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferer.shared.events.EventBus;
import com.transferer.shared.events.EventPublisher;
import com.transferer.shared.events.KafkaEventBus;
import com.transferer.shared.events.OutboxToKafkaEventBridge;
import com.transferer.shared.outbox.OutboxEventBus;
import com.transferer.shared.outbox.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaBridgeTestConfiguration {
    
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    private static final String TEST_TOPIC = "test-domain-events";
    
    static {
        if (!kafka.isRunning()) {
            kafka.start();
        }
    }
    
    private String getKafkaBootstrapServers() {
        // Ensure container is started before accessing ports
        if (!kafka.isRunning()) {
            kafka.start();
        }
        return kafka.getBootstrapServers();
    }
    
    @PreDestroy
    public void cleanup() {
        kafka.stop();
    }
    
    @Bean
    public OutboxEventBus outboxEventBus(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        return new OutboxEventBus(outboxEventRepository, objectMapper);
    }
    
    @Bean
    public ReactiveKafkaProducerTemplate<String, String> bridgeKafkaProducerTemplate() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);
        KafkaSender<String, String> kafkaSender = KafkaSender.create(senderOptions);
        
        return new ReactiveKafkaProducerTemplate<>(kafkaSender);
    }
    
    @Bean("kafkaEventBusProducerTemplate")
    public ReactiveKafkaProducerTemplate<String, String> kafkaEventBusProducerTemplate() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);
        KafkaSender<String, String> kafkaSender = KafkaSender.create(senderOptions);
        
        return new ReactiveKafkaProducerTemplate<>(kafkaSender);
    }
    
    @Bean
    public KafkaReceiver<String, String> kafkaReceiver() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-payment-saga");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(consumerProps)
                .subscription(Collections.singleton(TEST_TOPIC));
        
        return KafkaReceiver.create(receiverOptions);
    }

    
    @Bean
    public KafkaEventBus kafkaEventBus(ObjectMapper objectMapper) {
        return new KafkaEventBus(kafkaEventBusProducerTemplate(), kafkaReceiver(), objectMapper, TEST_TOPIC);
    }

    @Bean
    public EventPublisher eventPublisher(KafkaEventBus kafkaEventBus) {
        return kafkaEventBus;
    }
}