package by.rayden.paracoder.service;

import by.rayden.paracoder.config.PatternProperties;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor
public class RecodeCommand {
    /**
     * Map to replace invalid characters in file name to HomoGlyphs
     */
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final Map<Character, Character> SANITIZE_FILENAME_MAP = Map.of(
        '\t', ' ',
        '"', '″',
        '/', '╱',
        '\\', '⧹',
        '|', '￨',
        '?', '？',
        ':', '∶',
        '*', '∗',
        '<', '˂',
        '>', '˃'
    );

    private static final DateTimeFormatter FFMPEG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final PatternProperties patternProperties;

    public RecodeCommand() {
        BindResult<PatternProperties> bindResult = bindObjectByYaml("pattern", PatternProperties.class,
            "paracoder_commands.yml");

        this.patternProperties = bindResult
            .orElseThrow(() -> new IllegalArgumentException("No Paracoder commands found"));
    }

    private BindResult<PatternProperties> bindObjectByYaml(String prefix, Class<PatternProperties> target,
                                                           String yamlPath) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new PathResource(yamlPath));
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

    public void RecodeCommand__() throws IOException {
//        Yaml yaml = new Yaml(constructor, representer);
//
//
//        Pattern properties = null;
//        String content = null;
//        try {
//             content = Files.readString(Path.of("paracoder_commands.yml"), StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        properties = yaml.load(content);
//        this.patternProperties = properties.getPattern();
//

        PropertySourceLoader ypsl = new YamlPropertySourceLoader();
        PropertySource<?> ps = ypsl.load("paracoder_commands.yml", new PathResource("paracoder_commands.yml"))
                                   .getFirst();
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(ps);

        PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
//        Pattern pattern = propertyResolver.getProperty("pattern", Pattern.class);
        // propertyResolver.getProperty("pattern.commandTemplate.flac")

//        this.patternProperties = Objects.requireNonNull(pattern).getPattern();

    }

    public String getCommand(Path filePath) {
        String extension = FilenameUtils.getExtension(filePath.toString());
        String commandTemplate = getCommandTemplate(extension.toLowerCase(), "any");

        return resolvePlaceholders(commandTemplate, filePath.toString());
    }

    public String getCommand(CueTrackPayload cueTrackPayload) {
        String audioFileExt = FilenameUtils.getExtension(cueTrackPayload.getAudioFilePath().toString());
        String commandTemplate = getCommandTemplate("cue_" + audioFileExt.toLowerCase(), "cue_any");

        return resolvePlaceholders(commandTemplate, cueTrackPayload);
    }

    private String getCommandTemplate(String extension, String defaultExt) {
        Map<String, String> commandTemplateMap = this.patternProperties.getCommandTemplate();
        return commandTemplateMap.containsKey(extension) ?
            commandTemplateMap.get(extension) : commandTemplateMap.get(defaultExt);
    }

    private String resolvePlaceholders(String commandTemplate, String filePath) {
        return commandTemplate.replace("{{F}}", filePath)
                              .replace("{{D}}", FilenameUtils.getPrefix(filePath))
                              .replace("{{P}}", FilenameUtils.getPath(filePath))
                              .replace("{{N}}", FilenameUtils.getBaseName(filePath));
    }

    private String resolvePlaceholders(String commandTemplate, CueTrackPayload trackPayload) {
        String filePath = trackPayload.getAudioFilePath().toString();
        final DecimalFormat numberFormater = new DecimalFormat("#00");

        // Required values for placeholders. Cannot be null.
        return resolvePlaceholders(commandTemplate, filePath)
            .replace("{{CUE_ST}}", trackPayload.getStartTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_ET}}", trackPayload.getEndTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_METADATA}}", makeFFMpegMetadata(trackPayload))
            .replace("{{CUE_NUM}}", numberFormater.format(trackPayload.getTrackNumber()))
            .replace("{{CUE_TITLE}}", sanitizeFileName(Objects.requireNonNull(trackPayload.getTitle())))
            .replace("{{CUE_ARTIST}}", sanitizeFileName(Objects.requireNonNull(trackPayload.getPerformer())));
    }

    @VisibleForTesting
    String makeFFMpegMetadata(CueTrackPayload trackPayload) {
        final DecimalFormat numberFormater = new DecimalFormat("#00");

        var metadata = new HashMap<String, Object>();

        metadata.put("ARTIST", trackPayload.getPerformer());
        metadata.put("ALBUM", trackPayload.getAlbum());
        metadata.put("TITLE", trackPayload.getTitle());
        metadata.put("TRACK", numberFormater.format(trackPayload.getTrackNumber()));
        metadata.put("TOTALTRACKS", numberFormater.format(trackPayload.getTotalTracks()));
        metadata.put("DISCNUMBER", trackPayload.getDiscNumber());
        metadata.put("TOTALDISCS", trackPayload.getTotalDiscs());
        metadata.put("GENRE", trackPayload.getGenre());
        metadata.put("DATE", trackPayload.getYear());
        metadata.put("COMMENT", trackPayload.getComment());
        metadata.put("DISCID", trackPayload.getDiscId());

        return metadata.entrySet().stream()
                       .filter(entry -> entry.getValue() != null && !entry.getValue().toString().isBlank())
                       .map(this::createMetadataCommand)
                       .collect(Collectors.joining(" ", " ", " "));
    }

    private String createMetadataCommand(Map.Entry<String, Object> entry) {
        return "-metadata " + entry.getKey() + "=" + "\"" + entry.getValue() + "\"";
    }

    private String sanitizeFileName(String name) {
        return name.chars()
                   .mapToObj(i -> (char) i)
                   .map(c -> SANITIZE_FILENAME_MAP.getOrDefault(c, c))
                   // .filter(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_')
                   .collect(Collector.of(
                       StringBuilder::new,
                       StringBuilder::append,
                       StringBuilder::append,
                       StringBuilder::toString));
    }

}
