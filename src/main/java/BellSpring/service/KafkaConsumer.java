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
                .concatMap(this::processRecord)
                .doOnError(e -> log.error("Ошибка в потоке консьюмера", e))
                .retry()
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
                .map(Long::parseLong)
                .flatMap(orderRepository::deleteById)
                .doOnSuccess(v -> {
                    log.trace("Удален заказ: {}", record.value());
                    record.receiverOffset().acknowledge();
                })
                .doOnError(e -> {
                    log.error("Ошибка удаления заказа {}: {}", record.value(), e.getMessage());
                })
                .then();
    }
}