package BellSpring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<String> sendToKafka(String msg_id, String unixtimestamp,
                                    String method, String path) {
        return Mono.fromCallable(() -> {
                    Map<String, String> hashMap = new HashMap<>();
                    hashMap.put("msg_id", msg_id);
                    hashMap.put("timestamp", unixtimestamp);
                    hashMap.put("method", method);
                    hashMap.put("uri", path);

                    kafkaTemplate.send("postedmessages", hashMap);
                    return "OK";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just("Error: " + e.getMessage()));
    }
}