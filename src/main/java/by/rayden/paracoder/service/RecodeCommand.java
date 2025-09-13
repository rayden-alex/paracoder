package by.rayden.paracoder.service;

import by.rayden.paracoder.config.PatternProperties;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;

@Service
public class RecodeCommand {
    /**
     * Map to replace invalid characters in file name to HomoGlyphs
     */
    private static final Map<Character, Character> SANITIZE_FILENAME_MAP = Map.of(
        '/', '╱',
        '\\', '﹨',
        '|', '￨',
        '?', '？',
        ':', '∶',
        '*', '∗',
        '<', '˂',
        '>', '˃'
    );

    private static final DateTimeFormatter FFMPEG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final PatternProperties patternProperties;

    public RecodeCommand(PatternProperties patternProperties) {
        this.patternProperties = patternProperties;
    }

    public String getCommand(File file) {
        String extension = FilenameUtils.getExtension(file.toString());
        String commandTemplate = getCommandTemplate(extension.toLowerCase());

        return makeCommandFromTemplate(commandTemplate, file.toString());
    }

    public String getCommand(CueTrackPayload cueTrackPayload) {
        String audioFileExt = FilenameUtils.getExtension(cueTrackPayload.getAudioFilePath().toString());
        String commandTemplate = getCommandTemplate("cue_" + audioFileExt.toLowerCase());

        return makeCommandFromTemplate(commandTemplate, cueTrackPayload);
    }

    private String getCommandTemplate(String extension) {
        Map<String, String> commandTemplateMap = this.patternProperties.getCommandTemplate();
        return commandTemplateMap.containsKey(extension) ?
            commandTemplateMap.get(extension) : commandTemplateMap.get("any");
    }

    private String makeCommandFromTemplate(String commandTemplate, String filePath) {
        return commandTemplate.replace("{{F}}", filePath)
                              .replace("{{D}}", FilenameUtils.getPrefix(filePath))
                              .replace("{{P}}", FilenameUtils.getPath(filePath))
                              .replace("{{N}}", FilenameUtils.getBaseName(filePath));
    }

    private String makeCommandFromTemplate(String commandTemplate, CueTrackPayload trackPayload) {
        String filePath = trackPayload.getAudioFilePath().toString();
        final DecimalFormat numberFormater = new DecimalFormat("#00");

        return makeCommandFromTemplate(commandTemplate, filePath)
            .replace("{{CUE_ST}}", trackPayload.getStartTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_ET}}", trackPayload.getEndTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_NUM}}", numberFormater.format(trackPayload.getSongNumber()))
            .replace("{{CUE_TITLE}}", sanitizeFileName(Objects.requireNonNull(trackPayload.getTitle())));
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
