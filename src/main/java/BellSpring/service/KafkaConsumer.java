package BellSpring.service;

import BellSpring.model.MessageEntity;
import BellSpring.repository.MessageRepository;
import BellSpring.repository.OrderRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")  //  Вызываются фреймворком Spring при получении сообщений из Kafka
public class KafkaConsumer {

    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final ObservationRegistry observationRegistry;  // + 1 поле


    @KafkaListener(topics = "practicalwork", groupId = "del-messege")
    @Timed(value = "kafka.consume.practicalwork",
            description = "Time to consume and delete order from practicalwork topic")
    public void delmessege(String message) {

        Observation.createNotStarted("kafka.receive.delete", observationRegistry)
                .lowCardinalityKeyValue("topic", "practicalwork")
                .observe(() -> {

                    Long id = Long.parseLong(message);
                    orderRepository.deleteById(id);
                    log.info("Delete message: {}", message);
                });
    }
}
