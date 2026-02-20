package BellSpring.controller;

import BellSpring.model.DelayConfig;
import BellSpring.service.DelayService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delay")
@RequiredArgsConstructor
public class DelayController {

    private final DelayService delayService;

    @GetMapping
    @Timed(value = "delay.get.config",
            description = "Time taken to get delay configuration")
    public DelayConfig getDelays() {
        return delayService.getConfig();
    }

    @PostMapping
    @Timed(value = "delay.update.config",
            description = "Time taken to update delay configuration")
    public ResponseEntity<String> updateDelays(@RequestBody DelayConfig newConfig) {
        // В реальном приложении здесь была бы логика обновления конфигурации
        return ResponseEntity.ok("Use application.yml to change delays permanently");
    }
}
