package BellSpring.service;

import BellSpring.model.MessageEntity;
import BellSpring.repository.MessageRepository;
import BellSpring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;

    @KafkaListener(topics = "postedmessages", groupId = "my-group")
    public void listen(String message) {
        log.debug("Received message: {}", message);

        // Реактивное сохранение с использованием bounded elastic scheduler
        Mono.fromCallable(() -> {
                    MessageEntity entity = new MessageEntity();
                    entity.setContent(message);
                    return messageRepository.save(entity);
                })
                .subscribeOn(Schedulers.boundedElastic()) // Выполняем в отдельном пуле потоков
                .doOnSuccess(entity ->
                        log.debug("Message saved to database with id: {}", entity.getId())
                )
                .doOnError(error ->
                        log.error("Failed to save message: {}", error.getMessage())
                )
                .subscribe(); // Запускаем подписку
    }

    @KafkaListener(topics = "practicalwork", groupId = "del-messege")
    public void delmessege(String message) {
        log.debug("Delete order message received: {}", message);

        try {
            Long id = Long.parseLong(message);

            Mono.fromCallable(() -> {
                        orderRepository.deleteById(id);
                        return id;
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSuccess(deletedId ->
                            log.debug("Order deleted with id: {}", deletedId)
                    )
                    .doOnError(error ->
                            log.error("Failed to delete order with id {}: {}", id, error.getMessage())
                    )
                    .subscribe();

        } catch (NumberFormatException e) {
            log.error("Invalid message format for delete: {}", message);
        }
    }
}