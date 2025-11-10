package BellSpring.controller;

import BellSpring.model.DelayConfig;
import BellSpring.service.DelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delay")
@RequiredArgsConstructor
public class DelayController {

    private final DelayService delayService;

    @GetMapping
    public DelayConfig getDelays() {
        return delayService.getConfig();
    }

    @PostMapping
    public ResponseEntity<String> updateDelays(@RequestBody DelayConfig newConfig) {
        // В реальном приложении здесь была бы логика обновления конфигурации
        return ResponseEntity.ok("Use application.yml to change delays permanently");
    }
}
