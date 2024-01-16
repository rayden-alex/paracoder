package by.rayden.paracoder.service;

import by.rayden.paracoder.cli.command.ParaCoderParams;
import by.rayden.paracoder.config.PatternProperties;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class RecoderService {
    private final PatternProperties patternProperties;
    private ParaCoderParams paraCoderParams;

    public RecoderService(PatternProperties patternProperties) {
        this.patternProperties = patternProperties;
    }

    public int recode(ParaCoderParams paraCoderParams) {
        this.paraCoderParams = paraCoderParams;

        var pathMap = buildPathTree(this.paraCoderParams.inputPathList());
        processFiles(pathMap, this::processFile);
        processDirs(pathMap, this::processDir);

        return CommandLine.ExitCode.OK;
    }

    @NonNull
    private Map<Path, BasicFileAttributes> buildPathTree(final Iterable<? extends Path> inputPathList) {
        var fileVisitor = new CollectToMapFileVisitor(this.patternProperties.getFileExtensions());
        int depth = this.paraCoderParams.recurse() ? Integer.MAX_VALUE : 1;

        inputPathList.forEach(inputPath -> {
            try {
                Files.walkFileTree(inputPath, Collections.emptySet(), depth, fileVisitor);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return fileVisitor.getPathTree();
    }

    private void processFiles(Map<Path, BasicFileAttributes> pathMap,
                              Consumer<Map.Entry<Path, BasicFileAttributes>> processFileConsumer) {
        pathMap.entrySet().stream()
                .filter(entry -> entry.getValue().isRegularFile())
                .sorted(Map.Entry.comparingByKey()).forEach(processFileConsumer);
    }

    private void processDirs(final Map<Path, BasicFileAttributes> pathMap, Consumer<Map.Entry<Path,
        BasicFileAttributes>> processDirConsumer) {
        pathMap.entrySet().stream()
                .filter(entry -> entry.getValue().isDirectory())
                .sorted(Map.Entry.comparingByKey()).forEach(processDirConsumer);
    }

    private void processFile(Map.Entry<Path, BasicFileAttributes> entry) {
        Path filePath = entry.getKey();
        File oldFile = filePath.toFile();
        BasicFileAttributes fileAttributes = entry.getValue();

        System.out.println(CommandLine.Help.Ansi.ON.string(STR."Proceccing file ... : @|blue \{filePath}|@"));
        File newFile = filePath.toFile();

        if (this.paraCoderParams.preserveFileTimestamp()) {
            // see java.nio.file.attribute.BasicFileAttributeView.setTimes

            //noinspection ResultOfMethodCallIgnored
            newFile.setLastModified(fileAttributes.lastModifiedTime().toMillis());
        }
    }

    private void processDir(Map.Entry<Path, BasicFileAttributes> entry) {
        Path dirPath = entry.getKey();
        File dir = dirPath.toFile();
        BasicFileAttributes fileAttributes = entry.getValue();

        System.out.println(CommandLine.Help.Ansi.ON.string(STR."Proceccing dir ... : @|cyan \{dirPath}|@"));

        long oldLastModifiedTime = fileAttributes.lastModifiedTime().toMillis();
        if (this.paraCoderParams.preserveDirTimestamp() && dir.lastModified() != oldLastModifiedTime) {
            // see java.nio.file.attribute.BasicFileAttributeView.setTimes

            //noinspection ResultOfMethodCallIgnored
            dir.setLastModified(oldLastModifiedTime);
        }
    }
}
