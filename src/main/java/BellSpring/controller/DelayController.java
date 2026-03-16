package BellSpring.controller;

import BellSpring.model.DelayConfig;
import BellSpring.service.DelayService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.Map;

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
    public ResponseEntity<String> updateDelays(@RequestBody Map<String, Object> request) {
        DelayConfig currentConfig = delayService.getConfig();
        updateFields(currentConfig, request);
        return ResponseEntity.ok("Configuration updated temporarily!");
    }

    private void updateFields(Object target, Map<String, Object> source) {
        if (target == null || source == null) return;

        for (String key : source.keySet()) {
            try {
                Field field = target.getClass().getDeclaredField(key);
                field.setAccessible(true);
                Object value = source.get(key);

                if (value instanceof Map) {
                    Object nestedTarget = field.get(target);
                    if (nestedTarget != null) {
                        updateFields(nestedTarget, (Map<String, Object>) value);
                    }
                } else {
                    if (value instanceof Number) {
                        field.set(target, ((Number) value).longValue());
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // игнорируем
            }
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetDelays() {
        delayService.resetToOriginal();
        return ResponseEntity.ok("Configuration reset to original yml values!");
    }
}
