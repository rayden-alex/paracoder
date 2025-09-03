package by.rayden.paracoder.service;

import by.rayden.paracoder.cli.command.CommandController;
import by.rayden.paracoder.config.PatternProperties;
import by.rayden.paracoder.utils.OutUtils;
import by.rayden.paracoder.win32native.OsNative;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RecoderService {
    public static final Comparator<Path> REVERSED_PATH_COMPARATOR = Comparator
        .comparingInt(Path::getNameCount)
        .reversed();

    // https://javascript.info/regexp-greedy-and-lazy#alternative-approach
    public static final Pattern LAST_QUOTED_STRING_PATTERN = Pattern.compile("\"(?<targetFile>[^\"]+?)\"$");

    private final PatternProperties patternProperties;
    private final ProcessRunner processRunner;
    private final RecodeCommand recodeCommand;
    private final OsNative osNative;

    private CommandController.Params paraCoderParams;

    public RecoderService(ProcessRunner processRunner, RecodeCommand recodeCommand,
                          PatternProperties patternProperties, OsNative osNative) {
        this.processRunner = processRunner;
        this.recodeCommand = recodeCommand;
        this.patternProperties = patternProperties;
        this.osNative = osNative;
    }

    /**
     * Not thread safe! (because of this.paraCoderParams)
     */
    public int recode(CommandController.Params paraCoderParams) {
        this.paraCoderParams = paraCoderParams;

        if (!validateParams()) {
            return CommandLine.ExitCode.USAGE;
        }

        try {
            Map<Path, BasicFileAttributes> pathMap = buildAbsolutePathTree();
            int maxExitCode = asyncProcessFiles(pathMap);
            processDirs(pathMap);

            System.out.println();
            OutUtils.ansiOut("@|blue Max exit code: " + maxExitCode + "|@");
            return maxExitCode;
        } catch (Exception e) {
            log.error("Recode error: {}", e.getMessage(), e);
            OutUtils.ansiErr("Error: @|red " + e.getMessage() + "|@");
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    private boolean validateParams() {
        boolean valid = true;

        if (this.paraCoderParams.inputPathList().isEmpty()) {
            log.error("No files or directories have been selected to recode");
            OutUtils.ansiErr("Error: @|red No files or directories have been selected to recode|@");
            valid = false;
        }

        return valid;
    }

    private Map<Path, BasicFileAttributes> buildAbsolutePathTree() throws IOException {
        var fileVisitor = new CollectToMapFileVisitor(this.patternProperties.getFileExtensions());
        int depth = this.paraCoderParams.recurse() ? Integer.MAX_VALUE : 1;
        Set<FileVisitOption> fileVisitOptions = Collections.emptySet();

        for (Path inputPath : this.paraCoderParams.inputPathList()) {
            Files.walkFileTree(inputPath, fileVisitOptions, depth, fileVisitor);
        }
        log.debug("PathTree: {}", fileVisitor.getPathTree().keySet());
        return fileVisitor.getPathTree();
    }

    private int asyncProcessFiles(Map<Path, BasicFileAttributes> pathMap) throws InterruptedException,
        ExecutionException, TimeoutException {

        List<CompletableFuture<Integer>> futures = processFiles(pathMap);

        // Waiting for all processes to complete
        CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .get(1, TimeUnit.HOURS);

        // At this point all futures must convert their exception to integer exit codes.
        // See oneFileProcessResultAction()
        return futures.stream()
                      .map(CompletableFuture::join)
                      .reduce(Math::max)
                      .orElse(CommandLine.ExitCode.OK);
    }

    private List<CompletableFuture<Integer>> processFiles(Map<Path, BasicFileAttributes> pathMap) {
        return pathMap.entrySet().stream()
                      .filter(entry -> entry.getValue().isRegularFile())
                      .sorted(Map.Entry.comparingByKey())
                      .map(this::processFile)
                      .toList();
    }

    /**
     * The directories must be processed in reversed orders, starting at the deepest depth.
     */
    private void processDirs(final Map<Path, BasicFileAttributes> pathMap) {
        System.out.println();
        pathMap.entrySet().stream()
               .filter(entry -> entry.getValue().isDirectory())
               .sorted(Map.Entry.comparingByKey(REVERSED_PATH_COMPARATOR))
               .forEach(this::processDir);
    }

    @SneakyThrows
    private CompletableFuture<Integer> processFile(Map.Entry<Path, BasicFileAttributes> entry) {
        Path sourceFilePath = entry.getKey();
        FileTime sourceFileTime = entry.getValue().lastModifiedTime();

        if (!sourceFilePath.toFile().exists()) {
            OutUtils.ansiErr("Can't process source file: @|red " + sourceFilePath + "|@");
            return CompletableFuture.completedFuture(CommandLine.ExitCode.SOFTWARE);
        }

        String command = this.recodeCommand.getCommand(sourceFilePath);

        return this.processRunner
                   .execCommandAsync(command, sourceFilePath)
                   .orTimeout(10, TimeUnit.MINUTES) //??
                   .thenApply(preserveTimestampAction(command, sourceFileTime))
                   .thenApply(removeToTrashAction(sourceFilePath))
                   .whenComplete(oneFileProcessCompleteAction(sourceFilePath))
                   .handle(oneFileProcessResultAction());
    }

    private Function<Integer, Integer> preserveTimestampAction(String command, FileTime sourceFileTime) {
        return exitCode -> {
            if ((exitCode == CommandLine.ExitCode.OK) && this.paraCoderParams.preserveFileTimestamp()) {
                File file = getTargetFile(command);
                if (!setFileLastModifiedTime(file, sourceFileTime)) {
                    throw new RuntimeException("Error on setting timestamp to target file " + file);
                }
            }
            return exitCode;
        };
    }

    private Function<Integer, Integer> removeToTrashAction(Path sourceFilePath) {
        return exitCode -> {
            if ((exitCode == CommandLine.ExitCode.OK) && this.paraCoderParams.deleteSourceFilesToTrash()) {
                deleteFileToTrash(sourceFilePath);
            }
            return exitCode;
        };
    }

    private BiConsumer<Integer, Throwable> oneFileProcessCompleteAction(Path sourceFilePath) {
        return (exitCode, t) -> {
            if ((t == null) && (exitCode == CommandLine.ExitCode.OK)) {
                log.info("Completed OK {}", sourceFilePath);
                OutUtils.ansiOut("Completed: @|blue " + sourceFilePath + "|@");
            } else {
                log.error("Error on processing source file: {}", sourceFilePath, t);
                OutUtils.ansiErr(" @|red Error on processing source file: " + sourceFilePath + "|@");
            }
        };
    }


    /**
     * Convert exception to integer exit codes
     */
    private BiFunction<Integer, Throwable, Integer> oneFileProcessResultAction() {
        return (exitCode, t) -> {
            if (t == null) {
                return exitCode;
            } else {
                return CommandLine.ExitCode.SOFTWARE;
            }
        };
    }

    private File getTargetFile(String fileCommand) {
        var matcher = LAST_QUOTED_STRING_PATTERN.matcher(fileCommand);
        if (matcher.find()) {
            String targetFileName = matcher.group("targetFile");
            return Path.of(targetFileName).toFile();
        } else {
            throw new RuntimeException("Error on getting the target file from command " + fileCommand);
        }
    }

    /**
     * targetFile can be either a file or a directory
     *
     * @see BasicFileAttributeView#setTimes(FileTime, FileTime, FileTime)
     */
    private boolean setFileLastModifiedTime(File targetFile, FileTime sourceFileTime) {
        long sourceFileLastModifiedTime = sourceFileTime.toMillis();
        // See also: java.nio.file.attribute.BasicFileAttributeView.setTimes
        return (targetFile.lastModified() == sourceFileLastModifiedTime) || targetFile.setLastModified(sourceFileLastModifiedTime);
    }

    private void processDir(Map.Entry<Path, BasicFileAttributes> entry) {
        Path dirPath = entry.getKey();
        File dir = dirPath.toFile();
        FileTime sourceDirTime = entry.getValue().lastModifiedTime();

        if (this.paraCoderParams.preserveDirTimestamp()) {
            OutUtils.ansiOut("Processing dir: @|cyan " + dirPath + "|@");
            setFileLastModifiedTime(dir, sourceDirTime);
            log.info("Completed dir OK {}", dirPath);
        }
    }

    @SneakyThrows
    private void deleteFileToTrash(Path path) {
        this.osNative.deleteToTrash(path.toFile());
    }

}
