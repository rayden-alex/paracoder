package by.rayden.paracoder.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "pattern")
@PropertySource(value = "file:${commands:paracoder_commands.yml}", factory = YamlPropertySourceFactory.class)
@Getter
@Setter
public class PatternProperties {
    private Set<String> fileExtensions;
    private Map<String, String> command;
}
