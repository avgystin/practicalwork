package BellSpring.controller;

import BellSpring.model.DelayConfig;
import BellSpring.service.DelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/delay")
@RequiredArgsConstructor
public class DelayController {

    private final DelayService delayService;

    @GetMapping("/getDelays")  // Добавьте путь
    public Map<String, Long> getDelays() {
        return delayService.getConfig().getDelays();
    }

    @PostMapping
    public ResponseEntity<String> updateDelays(@RequestBody Map<String, Long> newDelays) {
        delayService.updateDelays(newDelays);
        return ResponseEntity.ok("Delays updated successfully");
    }

}
