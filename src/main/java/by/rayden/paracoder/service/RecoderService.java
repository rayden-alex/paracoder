package by.rayden.paracoder.service;

import by.rayden.paracoder.cli.command.ParaCoderParams;
import by.rayden.paracoder.config.PatternProperties;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecoderService {
    public static final Comparator<Path> REVERSED_PATH_COMPARATOR = Comparator
        .comparingInt(Path::getNameCount)
        .reversed()
        .thenComparing(Comparator.naturalOrder());

    // https://javascript.info/regexp-greedy-and-lazy#alternative-approach
    public static final Pattern LAST_QUOTED_STRING_PATTERN = Pattern.compile("\"(?<newFile>[^\"]+?)\"$");

    private final PatternProperties patternProperties;
    private ParaCoderParams paraCoderParams;

    public RecoderService(PatternProperties patternProperties) {
        this.patternProperties = patternProperties;
    }

    public int recode(ParaCoderParams paraCoderParams) {
        this.paraCoderParams = paraCoderParams;

        try {
            Map<Path, BasicFileAttributes> pathMap = buildAbsolutePathTree(this.paraCoderParams.inputPathList());
            processFiles(pathMap, this::processFile);
            processDirs(pathMap, this::processDir);
        } catch (Exception e) {
            System.err.println(CommandLine.Help.Ansi.ON.string(STR."Error: @|red \{e.getMessage()}|@"));
            return CommandLine.ExitCode.SOFTWARE;
        }

        return CommandLine.ExitCode.OK;
    }

    @NonNull
    private Map<Path, BasicFileAttributes> buildAbsolutePathTree(final Iterable<? extends Path> inputPathList) throws IOException {
        var fileVisitor = new CollectToMapFileVisitor(this.patternProperties.getFileExtensions());
        int depth = this.paraCoderParams.recurse() ? Integer.MAX_VALUE : 1;
        Set<FileVisitOption> fileVisitOptions = Collections.emptySet();

        for (Path inputPath : inputPathList) {
            Files.walkFileTree(inputPath, fileVisitOptions, depth, fileVisitor);
        }
        return fileVisitor.getPathTree();
    }

    private void processFiles(Map<Path, BasicFileAttributes> pathMap,
                              Consumer<Map.Entry<Path, BasicFileAttributes>> processFileConsumer) {
        pathMap.entrySet().stream()
                .filter(entry -> entry.getValue().isRegularFile())
                .sorted(Map.Entry.comparingByKey()).forEach(processFileConsumer);
    }

    /**
     * The directories must be processed in reversed orders, starting at the deepest depth.
     */
    private void processDirs(final Map<Path, BasicFileAttributes> pathMap,
                             Consumer<Map.Entry<Path, BasicFileAttributes>> processDirConsumer) {

        pathMap.entrySet().stream()
               .filter(entry -> entry.getValue().isDirectory())
               .sorted(Map.Entry.comparingByKey(REVERSED_PATH_COMPARATOR)).forEach(processDirConsumer);
    }

    @SneakyThrows
    private void processFile(Map.Entry<Path, BasicFileAttributes> entry) {
        Path filePath = entry.getKey();
        BasicFileAttributes fileAttributes = entry.getValue();

        if (filePath.toFile().exists()) {
            System.out.println(CommandLine.Help.Ansi.ON.string(STR."Processing file: @|blue \{filePath}|@"));
        } else {
            System.err.println(CommandLine.Help.Ansi.ON.string(STR."Can't process file: @|red \{filePath}|@"));
            return;
        }

        String fileCommand = getFileCommand(filePath);
        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command("cmd", "/c", fileCommand);
        Process process = processBuilder.start();

        boolean isCompleted = process.waitFor(5, TimeUnit.MINUTES);
        int exitValue = process.exitValue();

//        BlockingQueue<Runnable> queue = new SynchronousQueue<>();
//        ExecutorService executorService = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, queue);

        if (!isCompleted || exitValue != 0) {
            throw new RuntimeException(STR."Error on processing file: \{filePath}");
        }

        if (this.paraCoderParams.preserveFileTimestamp()) {
            long oldFileLastModifiedTime = fileAttributes.lastModifiedTime().toMillis();

            Optional<File> newFile = getNewFile(fileCommand);
            if (newFile.isPresent() && newFile.get().exists() && newFile.get().lastModified() != oldFileLastModifiedTime) {
                // see java.nio.file.attribute.BasicFileAttributeView.setTimes

                //noinspection ResultOfMethodCallIgnored
                newFile.get().setLastModified(fileAttributes.lastModifiedTime().toMillis());
            }
        }
    }

    private Optional<File> getNewFile(String fileCommand) {
        Matcher matcher = LAST_QUOTED_STRING_PATTERN.matcher(fileCommand);
        File newFile = null;
        if (matcher.find()) {
            String newFileName = matcher.group("newFile");
            newFile = Path.of(newFileName).toFile();
        }
        return Optional.ofNullable(newFile);
    }

    private String getFileCommand(Path filePath) {
        String extension = FilenameUtils.getExtension(filePath.getFileName().toString().toLowerCase());
        String commandTemplate = getCommandTemplate(extension);

        return irrigateCommand(commandTemplate, filePath.toString());
    }

    @NonNull
    private String irrigateCommand(String commandTemplate, String filePath) {
        commandTemplate = commandTemplate.replace("{{F}}", filePath)
                                         .replace("{{D}}", FilenameUtils.getPrefix(filePath))
                                         .replace("{{P}}", FilenameUtils.getPath(filePath))
                                         .replace("{{N}}", FilenameUtils.getBaseName(filePath));

        return commandTemplate;
    }

    private String getCommandTemplate(String extension) {
        Map<String, String> commandMap = this.patternProperties.getCommand();
        return commandMap.containsKey(extension) ? commandMap.get(extension) : commandMap.get("any");
    }

    private void processDir(Map.Entry<Path, BasicFileAttributes> entry) {
        Path dirPath = entry.getKey();
        File dir = dirPath.toFile();
        BasicFileAttributes dirAttributes = entry.getValue();

        System.out.println(CommandLine.Help.Ansi.ON.string(STR."Processing dir: @|cyan \{dirPath}|@"));

        if (this.paraCoderParams.preserveDirTimestamp()) {
            long oldLastModifiedTime = dirAttributes.lastModifiedTime().toMillis();
            if (dir.lastModified() != oldLastModifiedTime) {
                // see java.nio.file.attribute.BasicFileAttributeView.setTimes

                //noinspection ResultOfMethodCallIgnored
                dir.setLastModified(oldLastModifiedTime);
            }
        }
    }
}
