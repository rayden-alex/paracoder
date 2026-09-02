package by.rayden.paracoder.service;

import by.rayden.paracoder.config.PatternProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
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

    private static final DateTimeFormatter FFMPEG_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("HH:mm:ss.SSS", Locale.ROOT);

    private final PatternProperties patternProperties;

    public String getCommand(Path filePath) {
        String extension = FilenameUtils.getExtension(filePath.toString());
        String commandTemplate = getCommandTemplate(extension.toLowerCase(), "any");

        return resolvePlaceholders(commandTemplate, filePath.toString());
    }

    public String getCommand(CueTrackPayload cueTrackPayload) {
        String audioFileExt = FilenameUtils.getExtension(cueTrackPayload.audioFilePath().toString());
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
        String filePath = trackPayload.audioFilePath().toString();
        final DecimalFormat numberFormater = new DecimalFormat("#00");

        // Required values for placeholders. Cannot be null.
        return resolvePlaceholders(commandTemplate, filePath)
            .replace("{{CUE_ST}}", trackPayload.startTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_ET}}", trackPayload.endTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_METADATA}}", makeFFMpegMetadata(trackPayload))
            .replace("{{CUE_NUM}}", numberFormater.format(trackPayload.trackNumber()))
            .replace("{{CUE_TITLE}}", sanitizeFileName(Objects.requireNonNull(trackPayload.title())))
            .replace("{{CUE_ARTIST}}", sanitizeFileName(Objects.requireNonNull(trackPayload.performer())));
    }

    @VisibleForTesting
    String makeFFMpegMetadata(CueTrackPayload trackPayload) {
        final DecimalFormat numberFormater = new DecimalFormat("#00");

        var metadata = new HashMap<String, @Nullable Object>();

        metadata.put("ARTIST", trackPayload.performer());
        metadata.put("ALBUM", trackPayload.album());
        metadata.put("TITLE", trackPayload.title());
        metadata.put("TRACK", numberFormater.format(trackPayload.trackNumber()));
        metadata.put("TOTALTRACKS", numberFormater.format(trackPayload.totalTracks()));
        metadata.put("DISCNUMBER", trackPayload.discNumber());
        metadata.put("TOTALDISCS", trackPayload.totalDiscs());
        metadata.put("GENRE", trackPayload.genre());
        metadata.put("DATE", trackPayload.year());
        metadata.put("COMMENT", trackPayload.comment());
        metadata.put("DISCID", trackPayload.discId());

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
