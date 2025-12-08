package BellSpring.service;

import BellSpring.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer implements DisposableBean {
    private final OrderRepository orderRepository;
    private final ReactiveKafkaConsumerTemplate<String, String> kafkaConsumerTemplate;
    private Disposable subscription;

    @PostConstruct
    public void startConsuming() {
        subscription = kafkaConsumerTemplate
                .receive()
                .flatMap(this::processRecord, 10)  // ← Параллельная обработка
                .onErrorResume(e -> {  // ← Восстанавливаем поток при ошибках
                    log.error("Ошибка в потоке консьюмера", e);
                    return Mono.empty();
                })
                .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofSeconds(5))) // ← Ограниченный retry
                .subscribe();
    }

    @Override
    public void destroy() {
        if (subscription != null) {
            subscription.dispose();
            log.info("Kafka consumer остановлен");
        }
    }

    private Mono<Void> processRecord(ReceiverRecord<String, String> record) {
        return Mono.just(record.value())
                .flatMap(orderIdStr -> {
                    try {
                        Long orderId = Long.parseLong(orderIdStr);
                        // Идемпотентная обработка
                        return orderRepository.findById(orderId)
                                .flatMap(order -> orderRepository.delete(order)
                                        .doOnSuccess(v -> {
                                            log.trace("Удален заказ: {}", orderId);
                                            record.receiverOffset().acknowledge();
                                        }))
                                .switchIfEmpty(Mono.fromRunnable(() -> {
                                    // Заказ уже удален - подтверждаем
                                    log.trace("Заказ уже удален: {}", orderId);
                                    record.receiverOffset().acknowledge();
                                }));
                    } catch (NumberFormatException e) {
                        log.error("Неверный формат ID заказа: {}", orderIdStr);
                        record.receiverOffset().acknowledge();  // Подтверждаем битое сообщение
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.error("Ошибка удаления заказа: {}", e.getMessage());
                    // НЕ подтверждаем при ошибке БД - будет повтор
                    return Mono.empty();
                })
                .then();
    }
}