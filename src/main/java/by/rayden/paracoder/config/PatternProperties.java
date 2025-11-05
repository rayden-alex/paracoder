package by.rayden.paracoder.config;

import by.rayden.paracoder.cli.ParacoderParamsReadyEvent;
import by.rayden.paracoder.cli.command.CommandController;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * The properties are stored in the external file {@link CommandController#getConfigPath()}
 * and loaded by the custom loader to use the path from the application commandline parameters.
 */
@Component
@Getter
@Setter
@Slf4j
public class PatternProperties {
    private static final String PATTERN_PROPERTIES_PREFIX = "pattern";

    private Set<String> fileExtensions;
    private Map<String, String> commandTemplate;

    // TODO: Need to put in order the names for this configuration name
    //  (configuration, template, pattern - it's confusing)
    @EventListener
    public void onParamsReadyEvent(ParacoderParamsReadyEvent event) {
        // Use Spring event functionality to get params value only when they are already parsed
        CommandController commandController = (CommandController) event.getSource();
        Path configPath = commandController.getConfigPath();

        loadConfig(configPath);
    }

    private void loadConfig(Path configPath) {
        try {
            log.debug("Loading Paracoder config file: {}", configPath);
            BindResult<PatternProperties> bindResult = bindObjectByYaml(PATTERN_PROPERTIES_PREFIX,
                PatternProperties.class, configPath);

            this.fileExtensions = bindResult.get().getFileExtensions();
            this.commandTemplate = bindResult.get().getCommandTemplate();
            log.info("Loaded config file: {}", configPath);
        } catch (Exception e) {
            log.error("Error on loading Paracoder config file: {}", configPath);
            throw new RuntimeException("Error on loading Paracoder config file!!!", e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private BindResult<PatternProperties> bindObjectByYaml(String prefix, Class<PatternProperties> target,
                                                           Path configPath) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new PathResource(configPath));
        Properties properties = Objects.requireNonNull(factory.getObject());

        return getBinder(properties).bind(prefix, target);
    }

    /**
     * @see org.springframework.boot.context.properties.ConfigurationPropertiesBinder#getBinder()
     * @see YamlPropertiesFactoryBean#createProperties()
     */
    @SuppressWarnings("JavadocReference")
    private Binder getBinder(Properties properties) {
        // A property source which will be used to assign JavaBean properties.
        var propertySource = new MapConfigurationPropertySource(properties);

        // A property source which will be used to resolve placeholders.
        // In this case, it is based on the same properties, since we do not want to use any other system properties,
        // properties from the environment and from other configuration files.
        var resolverPropertySource = new PropertiesPropertySource("mySource", properties);
        var placeholdersResolver = new PropertySourcesPlaceholdersResolver(List.of(resolverPropertySource));

        return new Binder(List.of(propertySource), placeholdersResolver);
    }

}
