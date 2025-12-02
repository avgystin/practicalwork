package BellSpring.controller;

import BellSpring.service.DelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/delay")
@RequiredArgsConstructor
public class DelayController {

    private final DelayService delayService;

    @GetMapping("/getDelays")
    public Mono<Map<String, Long>> getDelays() {
        return Mono.fromCallable(() ->
                delayService.getConfig().getDelays()
        );
    }

    @PostMapping
    public Mono<ResponseEntity<String>> updateDelays(@RequestBody Map<String, Long> newDelays) {
        return Mono.fromCallable(() -> {
            delayService.updateDelays(newDelays);
            return ResponseEntity.ok("Delays updated successfully");
        });
    }

}