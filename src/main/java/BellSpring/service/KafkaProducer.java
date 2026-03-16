//package BellSpring.service;
//
//import io.micrometer.core.annotation.Timed;
//import io.micrometer.observation.Observation;
//import io.micrometer.observation.ObservationRegistry;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class KafkaProducer {
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//    private final ObservationRegistry observationRegistry;

//    @Timed(value = "kafka.send",
//            description = "Time taken to send message to Kafka")
//    public ResponseEntity<String> sendToKafka(String msg_id, String unixtimestamp,
//                                              String method, String path) {
//
//        Map<String, String> hashMap = new HashMap<>();
//        hashMap.put("msg_id", msg_id);
//        hashMap.put("timestamp", unixtimestamp);
//        hashMap.put("method", method);
//        hashMap.put("uri", path);
//
//        return Observation.createNotStarted("kafka.send", observationRegistry)
//                .lowCardinalityKeyValue("topic", "postedmessages")
//                .observe(() -> {
//                    try {
//                        kafkaTemplate.send("postedmessages", hashMap);
//                        return ResponseEntity.ok("OK");
//                    } catch (Exception e) {
//                        return ResponseEntity.status(500).body("Error");
//                    }
//                });
//    }
//}
