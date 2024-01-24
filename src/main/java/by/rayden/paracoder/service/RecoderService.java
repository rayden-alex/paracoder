package by.rayden.paracoder.service;

import by.rayden.paracoder.cli.command.ParaCoderMainCommand;
import by.rayden.paracoder.config.PatternProperties;
import lombok.SneakyThrows;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecoderService {
    public static final Comparator<Path> REVERSED_PATH_COMPARATOR = Comparator
        .comparingInt(Path::getNameCount)
        .reversed()
        .thenComparing(Comparator.naturalOrder());

    // https://javascript.info/regexp-greedy-and-lazy#alternative-approach
    public static final Pattern LAST_QUOTED_STRING_PATTERN = Pattern.compile("\"(?<targetFile>[^\"]+?)\"$");

    private final PatternProperties patternProperties;
    private final ProcessFactory processFactory;
    private final RecodeCommand recodeCommand;

    private ParaCoderMainCommand.Params paraCoderParams;

    public RecoderService(ProcessFactory processFactory, RecodeCommand recodeCommand,
                          PatternProperties patternProperties) {
        this.processFactory = processFactory;
        this.recodeCommand = recodeCommand;
        this.patternProperties = patternProperties;
    }

    /**
     * Not thread safe! (because of this.paraCoderParams)
     */
    public int recode(ParaCoderMainCommand.Params paraCoderParams) {
        this.paraCoderParams = paraCoderParams;

        try {
            Map<Path, BasicFileAttributes> pathMap = buildAbsolutePathTree(this.paraCoderParams.inputPathList());

            List<CompletableFuture<Integer>> completableFutures = processFiles(pathMap);
            waitForComplete(completableFutures);

            Integer maxExitCode = completableFutures.stream()
                                                    .map(CompletableFuture::join)
                                                    .reduce(Math::max)
                                                    .orElse(CommandLine.ExitCode.OK);

            processDirs(pathMap);
            System.out.println(CommandLine.Help.Ansi.ON.string(STR."@|blue Max exit code: \{maxExitCode}|@"));
            return maxExitCode;
        } catch (Exception e) {
            System.err.println(CommandLine.Help.Ansi.ON.string(STR."Error: @|red \{e.getMessage()}|@"));
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    private void waitForComplete(List<CompletableFuture<Integer>> completableFutures) throws InterruptedException,
        ExecutionException, TimeoutException {

        CompletableFuture
            .allOf(completableFutures.toArray(new CompletableFuture[0]))
            .get(1, TimeUnit.HOURS);
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

    private List<CompletableFuture<Integer>> processFiles(Map<Path, BasicFileAttributes> pathMap) {
        return pathMap.entrySet().stream()
                      .filter(entry -> entry.getValue().isRegularFile())
                      .sorted(Map.Entry.comparingByKey())
                      .map(this::processFile)
                      .collect(Collectors.toList());
    }

    /**
     * The directories must be processed in reversed orders, starting at the deepest depth.
     */
    private void processDirs(final Map<Path, BasicFileAttributes> pathMap) {
        pathMap.entrySet().stream()
               .filter(entry -> entry.getValue().isDirectory())
               .sorted(Map.Entry.comparingByKey(REVERSED_PATH_COMPARATOR))
               .forEach(this::processDir);
    }

    @SneakyThrows
    private CompletableFuture<Integer> processFile(Map.Entry<Path, BasicFileAttributes> entry) {
        Path sourceFilePath = entry.getKey();
        BasicFileAttributes sourceFileAttributes = entry.getValue();

        if (!sourceFilePath.toFile().exists()) {
            System.err.println(CommandLine.Help.Ansi.ON.string(STR."Can't process source file: @|red \{sourceFilePath}|@"));
            return CompletableFuture.completedFuture(-1);
        }

        String command = this.recodeCommand.getCommand(sourceFilePath);

        CompletableFuture<Integer> process = this.processFactory.execCommandAsync(command, sourceFilePath);
        process.orTimeout(10, TimeUnit.MINUTES);

        if (this.paraCoderParams.preserveFileTimestamp()) {
            process.thenApply(preserveTimestampAction(command, sourceFileAttributes, sourceFilePath));
        }

        if (this.paraCoderParams.deleteSourceFilesToTrash()) {
            process.thenApply(removeToTrashAction(sourceFilePath));
        }

        process.whenComplete(oneFileProcessCompleteAction(sourceFilePath));
        process.handle(oneFileProcessResultAction());

        return process;
    }

    @NonNull
    private Function<Integer, Integer> preserveTimestampAction(String command,
                                                               BasicFileAttributes sourceFileAttributes,
                                                               Path sourceFilePath) {
        return exitCode -> getTargetFile(command)
            .map(file -> {
                setTargetFileLastModifiedTime(file, sourceFileAttributes);
                return exitCode;
            })
            .orElseThrow(() -> new RuntimeException(STR."Error on getting the target file for \{sourceFilePath}"));
    }

    @NonNull
    private Function<Integer, Integer> removeToTrashAction(Path sourceFilePath) {
        return exitCode -> {
            if (!removeFileToTrash(sourceFilePath)) {
                throw new RuntimeException(STR."Error on deleting source file to the trash \{sourceFilePath}");
            }
            return exitCode;
//            throw new RuntimeException(STR."Error on deleting source file to the trash \{sourceFilePath}");
        };
    }

    @NonNull
    private BiConsumer<Integer, Throwable> oneFileProcessCompleteAction(Path sourceFilePath) {
        return (exitCode, t) -> {
            if (t == null) {
//                logger.info("success: {}", exitCode);
                System.out.println(CommandLine.Help.Ansi.ON.string(STR."""
                    Completed: @|blue \{sourceFilePath}|@"""));
            } else {
//                logger.warn("failure: {}", t.getMessage());
                System.err.println(CommandLine.Help.Ansi.ON.string(STR."""
                Can't process source file: \{sourceFilePath}
                @|red \{t.getMessage()}|@"""));
            }
        };
    }

    @NonNull
    private BiFunction<Integer, Throwable, Integer> oneFileProcessResultAction() {
        return (exitCode, t) -> {
            if (t == null) {
                return exitCode;
            } else {
                return -1;
            }
        };
    }

    @NonNull
    private Optional<File> getTargetFile(String fileCommand) {
        var matcher = LAST_QUOTED_STRING_PATTERN.matcher(fileCommand);
        if (matcher.find()) {
            String targetFileName = matcher.group("targetFile");
            return Optional.of(Path.of(targetFileName).toFile());
        } else {
            return Optional.empty();
        }
    }

    /**
     * targetFile can be either a file or a directory
     */
    private void setTargetFileLastModifiedTime(File targetFile, BasicFileAttributes fileAttributes) {
        long sourceFileLastModifiedTime = fileAttributes.lastModifiedTime().toMillis();
        if (targetFile.lastModified() != sourceFileLastModifiedTime) {
            // See also: java.nio.file.attribute.BasicFileAttributeView.setTimes
            //noinspection ResultOfMethodCallIgnored
            targetFile.setLastModified(sourceFileLastModifiedTime);
        }
    }

    private void processDir(Map.Entry<Path, BasicFileAttributes> entry) {
        Path dirPath = entry.getKey();
        File dir = dirPath.toFile();
        BasicFileAttributes dirAttributes = entry.getValue();

        if (this.paraCoderParams.preserveDirTimestamp()) {
            System.out.println(CommandLine.Help.Ansi.ON.string(STR."Processing dir: @|cyan \{dirPath}|@"));
            setTargetFileLastModifiedTime(dir, dirAttributes);
        }
    }

    private boolean removeFileToTrash(Path path) {
//        Toolkit tk = Toolkit.getDefaultToolkit();
//        // Standard beep is available.
//        tk.beep();
        try {
            return Desktop.getDesktop().moveToTrash(path.toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
