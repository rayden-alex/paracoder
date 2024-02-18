package by.rayden.paracoder.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * The properties are stored in the external "paracoder_commands.yml" file
 * and loaded by using the "spring.config.import" property in the "application.yml".
 */
@Configuration
@ConfigurationProperties(prefix = "pattern")
@Getter
@Setter
public class PatternProperties {
    private Set<String> fileExtensions;
    private Map<String, String> commandTemplate;
}
