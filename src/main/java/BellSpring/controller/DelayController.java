package BellSpring.controller;

import BellSpring.model.DelayConfig;
import BellSpring.service.DelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> updateDelays(@RequestBody Map<String, Object> request) {
        DelayConfig currentConfig = delayService.getConfig();
        List<String> errors = new ArrayList<>();

        updateFields(currentConfig, request, errors);

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid fields: " + String.join(", ", errors)));
        }
        return ResponseEntity.ok("Configuration updated temporarily!");
    }

    private void updateFields(Object target, Map<String, Object> source, List<String> errors) {
        for (String key : source.keySet()) {
            try {
                Field field = target.getClass().getDeclaredField(key);
                field.setAccessible(true);
                Object value = source.get(key);

                if (value instanceof Map) {
                    updateFields(field.get(target), (Map<String, Object>) value, errors);
                } else if (value instanceof Number) {
                    field.set(target, ((Number) value).longValue());
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                errors.add(key);
            }
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetDelays() {
        delayService.resetToOriginal();
        return ResponseEntity.ok("Configuration reset to original yml values!");
    }
}
