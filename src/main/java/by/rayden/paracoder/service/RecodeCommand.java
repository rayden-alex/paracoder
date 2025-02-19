package by.rayden.paracoder.service;

import by.rayden.paracoder.config.PatternProperties;
import org.apache.commons.io.FilenameUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

@Service
public class RecodeCommand {
    private final PatternProperties patternProperties;

    public RecodeCommand(PatternProperties patternProperties) {
        this.patternProperties = patternProperties;
    }

    @NonNull
    public String getCommand(Path filePath) {
        String extension = FilenameUtils.getExtension(filePath.getFileName().toString());
        String commandTemplate = getCommandTemplate(extension.toLowerCase());

        return makeCommandFromTemplate(commandTemplate, filePath.toString());
    }

    @NonNull
    private String getCommandTemplate(String extension) {
        Map<String, String> commandTemplateMap = this.patternProperties.getCommandTemplate();
        return commandTemplateMap.containsKey(extension) ?
            commandTemplateMap.get(extension) : commandTemplateMap.get("any");
    }

    @NonNull
    private String makeCommandFromTemplate(String commandTemplate, String filePath) {
        return commandTemplate.replace("{{F}}", filePath)
                              .replace("{{D}}", FilenameUtils.getPrefix(filePath))
                              .replace("{{P}}", FilenameUtils.getPath(filePath))
                              .replace("{{N}}", FilenameUtils.getBaseName(filePath));
    }
}
