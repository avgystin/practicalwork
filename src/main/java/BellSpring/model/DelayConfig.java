package BellSpring.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "response.delay")
public class DelayConfig {

    private Map<String, Long> delays = new HashMap<>();

    public void updateDelays(Map<String, Long> newDelays) {
        this.delays.putAll(newDelays);
    }

    public Map<String, Long> getDelays() {
        return delays;
    }

}