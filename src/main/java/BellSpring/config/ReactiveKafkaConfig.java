package BellSpring.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ReactiveKafkaConfig {

    @Bean
    public ReceiverOptions<String, String> receiverOptions(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "del-message-reactive");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Явное указание типов через ReceiverOptions.create
        ReceiverOptions<String, String> receiverOptions =
                ReceiverOptions.<String, String>create(props)
                        .subscription(Collections.singletonList("practicalwork"));

        return receiverOptions;
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, String> kafkaConsumerTemplate( // ← переименуй
                                                                                ReceiverOptions<String, String> receiverOptions) {
        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }
}