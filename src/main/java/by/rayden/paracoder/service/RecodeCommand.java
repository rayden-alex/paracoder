package by.rayden.paracoder.service;

import by.rayden.paracoder.config.PatternProperties;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
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

    public RecodeCommand(PatternProperties patternProperties) {
        this.patternProperties = patternProperties;
    }

    public String getCommand(File file) {
        String extension = FilenameUtils.getExtension(file.toString());
        String commandTemplate = getCommandTemplate(extension.toLowerCase());

        return fillCommandPlaceholders(commandTemplate, file.toString());
    }

    public String getCommand(CueTrackPayload cueTrackPayload) {
        String audioFileExt = FilenameUtils.getExtension(cueTrackPayload.getAudioFilePath().toString());
        String commandTemplate = getCommandTemplate("cue_" + audioFileExt.toLowerCase());

        return fillCommandPlaceholders(commandTemplate, cueTrackPayload);
    }

    private String getCommandTemplate(String extension) {
        Map<String, String> commandTemplateMap = this.patternProperties.getCommandTemplate();
        return commandTemplateMap.containsKey(extension) ?
            commandTemplateMap.get(extension) : commandTemplateMap.get("any");
    }

    private String fillCommandPlaceholders(String commandTemplate, String filePath) {
        return commandTemplate.replace("{{F}}", filePath)
                              .replace("{{D}}", FilenameUtils.getPrefix(filePath))
                              .replace("{{P}}", FilenameUtils.getPath(filePath))
                              .replace("{{N}}", FilenameUtils.getBaseName(filePath));
    }

    private String fillCommandPlaceholders(String commandTemplate, CueTrackPayload trackPayload) {
        String filePath = trackPayload.getAudioFilePath().toString();
        final DecimalFormat numberFormater = new DecimalFormat("#00");

        return fillCommandPlaceholders(commandTemplate, filePath)
            .replace("{{CUE_ST}}", trackPayload.getStartTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_ET}}", trackPayload.getEndTime().format(FFMPEG_TIME_FORMATTER))
            .replace("{{CUE_METADATA}}", makeFFMpegMetadata(trackPayload))
            .replace("{{CUE_NUM}}", numberFormater.format(trackPayload.getTrackNumber()))
            .replace("{{CUE_TITLE}}", sanitizeFileName(Objects.requireNonNull(trackPayload.getTitle())));
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
                       .filter(entry -> entry.getValue() != null && !entry.getValue().toString().trim().isEmpty())
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
