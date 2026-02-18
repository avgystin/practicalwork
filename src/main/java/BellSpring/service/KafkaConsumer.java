package BellSpring.service;

import BellSpring.model.MessageEntity;
import BellSpring.repository.MessageRepository;
import BellSpring.repository.OrderRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final ObservationRegistry observationRegistry;  // + 1 поле

    @KafkaListener(topics = "postedmessages", groupId = "my-group")
    public void listen(String message) {

        Observation.createNotStarted("kafka.receive", observationRegistry)
                .lowCardinalityKeyValue("topic", "postedmessages")
                .observe(() -> {

                    System.out.println("Received message: " + message);

                    MessageEntity entity = new MessageEntity();
                    entity.setContent(message);
                    messageRepository.save(entity);

                    log.info("Message saved to database with id: {}", entity.getId());
                });
    }

    @KafkaListener(topics = "practicalwork", groupId = "del-messege")
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
