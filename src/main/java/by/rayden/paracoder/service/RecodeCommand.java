package by.rayden.paracoder.service;

import by.rayden.paracoder.config.PatternProperties;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

@Service
public class RecodeCommand {
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
        String commandTemplate = getCommandTemplate("cue");

        return makeCueCommandFromTemplate(commandTemplate, cueTrackPayload);
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

    private String makeCueCommandFromTemplate(String commandTemplate, CueTrackPayload trackPayload) {
        String filePath = Objects.requireNonNull(trackPayload.getAudioFilePath()).toString();
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        final DecimalFormat numberFormater = new DecimalFormat("#00");

        return makeCommandFromTemplate(commandTemplate, filePath)
            .replace("{{ST}}", Objects.requireNonNull(trackPayload.getStartTime()).format(timeFormatter)) //TODO
            .replace("{{ET}}", Objects.requireNonNull(trackPayload.getEndTime()).format(timeFormatter)) //TODO
            .replace("{{NUM}}", numberFormater.format(trackPayload.getSongNumber()))
            .replace("{{TITLE}}", Objects.requireNonNull(trackPayload.getTitle()));

    }
}
