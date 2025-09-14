package by.rayden.paracoder.service;

import by.rayden.paracoder.cli.command.CommandController;
import by.rayden.paracoder.config.PatternProperties;
import by.rayden.paracoder.utils.OutUtils;
import by.rayden.paracoder.win32native.OsNative;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.digitalmediaserver.cuelib.CueParser;
import org.digitalmediaserver.cuelib.CueSheet;
import org.digitalmediaserver.cuelib.FileData;
import org.digitalmediaserver.cuelib.Position;
import org.digitalmediaserver.cuelib.TrackData;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.digitalmediaserver.cuelib.CueSheet.MetaDataField;

@Service
@Slf4j
public class RecoderService {
    public static final Comparator<Path> REVERSED_PATH_COMPARATOR = Comparator
        .comparingInt(Path::getNameCount)
        .reversed();

    // https://javascript.info/regexp-greedy-and-lazy#alternative-approach
    public static final Pattern LAST_QUOTED_STRING_PATTERN = Pattern.compile("\"(?<targetFile>[^\"]+?)\"$");
    private static final LocalTime END_OF_FILE_TIME = LocalTime.of(23, 59, 59);

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
                      .flatMap(Collection::stream)
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

    /**
     * When processing one "CUE" file, several output files may be generated.
     * <p>That's why the result of the method is a List.
     */
    @SneakyThrows
    private List<CompletableFuture<Integer>> processFile(Map.Entry<Path, BasicFileAttributes> entry) {
        File sourceFile = entry.getKey().toFile();
        FileTime sourceFileTime = entry.getValue().lastModifiedTime();

        if (!sourceFile.exists()) {
            OutUtils.ansiErr("Source file doesn't exists: @|red " + sourceFile + "|@");
            return Collections.singletonList(CompletableFuture.completedFuture(CommandLine.ExitCode.SOFTWARE));
        }

        String extension = FilenameUtils.getExtension(sourceFile.toString());
        if ("cue".equalsIgnoreCase(extension)) {
            return createFuturesForCueFile(sourceFile);
        } else {
            return Collections.singletonList(createFutureForOrdinalFile(sourceFile, sourceFileTime));
        }
    }

    private List<CompletableFuture<Integer>> createFuturesForCueFile(File sourceFile) {
        try {
            CueSheet cue = CueParser.parse(sourceFile.toPath(), StandardCharsets.UTF_8);

            return cue.getFileData().stream()
                      .map(FileData::getTrackData)
                      .flatMap(Collection::stream)
                      .filter(trackData -> "AUDIO".equalsIgnoreCase(trackData.getDataType())) // TODO: Do I need this ?
                      .map(trackData -> getCueTrackPayload(trackData, sourceFile))
                      .map(this::createFutureForCueTrack)
                      .toList();
        } catch (Exception e) {
            return Collections.singletonList(CompletableFuture.failedFuture(e));
        }
    }

    private CompletableFuture<Integer> createFutureForOrdinalFile(File sourceFile, FileTime sourceFileTime) {
        String command = this.recodeCommand.getCommand(sourceFile);
        File targetFile = getTargetFile(command);

        return this.processRunner
            .execCommandAsync(command, sourceFile)
            .orTimeout(10, TimeUnit.MINUTES) //??
            .thenApply(preserveTimestampAction(targetFile, sourceFileTime))
            .thenApply(removeToTrashAction(sourceFile))
            .whenComplete(oneFileProcessCompleteAction(sourceFile))
            .handle(oneFileProcessResultAction());
    }

    @SneakyThrows
    private CueTrackPayload getCueTrackPayload(TrackData trackData, File sourceFile) {
        LocalTime trackStartTime = convertToTime(trackData.getStartIndex()
                                                          .getPosition()); // TODO: Do I need a PreGap processing ?

        LocalTime trackEndTime;
        if (trackData == trackData.getParent().getTrackData().getLast()) {
            trackEndTime = END_OF_FILE_TIME; // to the end of file
        } else {
            int nextTrackIndex = trackData.getNumber();
            TrackData nextTrackData = trackData.getParent().getTrackData().get(nextTrackIndex);
            trackEndTime = convertToTime(nextTrackData.getStartIndex().getPosition());
        }

        // It is assumed that the Audio file is located next to the source CUE file
        Path audioFilePath = sourceFile.toPath().resolveSibling(trackData.getParent().getFile());
        FileTime audioLastModifiedTime = Files.getLastModifiedTime(audioFilePath);
        CueSheet cueSheet = trackData.getParent().getParent();

        return CueTrackPayload
            .builder()
            .trackNumber(trackData.getNumber())
            .totalTracks(trackData.getParent().getTrackData().size())
            .title(trackData.getTitle())
            .performer(trackData.getMetaData(MetaDataField.PERFORMER))
            .album(cueSheet.getTitle())
            .year(cueSheet.getYear() != -1 ? cueSheet.getYear() : null)
            .genre(cueSheet.getGenre())
            .comment(cueSheet.getComment())
            .discId(cueSheet.getDiscId())
            .discNumber(cueSheet.getDiscNumber()!= -1 ? cueSheet.getDiscNumber() : null)
            .totalDiscs(cueSheet.getTotalDiscs()!= -1 ? cueSheet.getTotalDiscs() : null)
            .startTime(trackStartTime)
            .endTime(trackEndTime)
            .sourceFile(sourceFile)
            .audioFilePath(audioFilePath)
            .audioFileTime(audioLastModifiedTime)
            .build();
    }

    private LocalTime convertToTime(@Nullable Position position) {
        Objects.requireNonNull(position);

        final int SECONDS_PER_MINUTE = 60;
        final long NANOS_PER_SECOND = 1000_000_000L;
        final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;

        // CD Audio (Red Book) has 75 frames per second
        final int FRAMES_PER_SECOND = 75;
        final long NANOS_PER_FRAME = NANOS_PER_SECOND / FRAMES_PER_SECOND;

        return LocalTime.ofNanoOfDay(position.getMinutes() * NANOS_PER_MINUTE
            + position.getSeconds() * NANOS_PER_SECOND
            + position.getFrames() * NANOS_PER_FRAME);
    }

    private CompletableFuture<Integer> createFutureForCueTrack(CueTrackPayload trackPayload) {
        String command = this.recodeCommand.getCommand(trackPayload);
        File targetFile = getTargetFile(command);

        return this.processRunner.execCommandAsync(command, trackPayload.getSourceFile())
                                 .orTimeout(10, TimeUnit.MINUTES) //TODO
                                 .thenApply(preserveTimestampAction(targetFile, trackPayload.getAudioFileTime()))
                                 .whenComplete(oneFileProcessCompleteAction(targetFile)) // TODO:
                                 .handle(oneFileProcessResultAction());
    }

    private Function<Integer, Integer> preserveTimestampAction(File targetFile, FileTime sourceFileTime) {
        return exitCode -> {
            if ((exitCode == CommandLine.ExitCode.OK) && this.paraCoderParams.preserveFileTimestamp()) {
                if (!setFileLastModifiedTime(targetFile, sourceFileTime)) {
                    throw new RuntimeException("Error on setting timestamp to target targetFile " + targetFile);
                }
            }
            return exitCode;
        };
    }

    private Function<Integer, Integer> removeToTrashAction(File sourceFile) {
        return exitCode -> {
            if ((exitCode == CommandLine.ExitCode.OK) && this.paraCoderParams.deleteSourceFilesToTrash()) {
                deleteFileToTrash(sourceFile);
            }
            return exitCode;
        };
    }

    private BiConsumer<Integer, Throwable> oneFileProcessCompleteAction(File sourceFile) {
        return (exitCode, t) -> {
            if ((t == null) && (exitCode == CommandLine.ExitCode.OK)) {
                log.info("Completed OK {}", sourceFile);
                OutUtils.ansiOut("Completed: @|blue " + sourceFile + "|@");
            } else {
                log.error("Error on processing source file: {}", sourceFile, t);
                OutUtils.ansiErr(" @|red Error on processing source file: " + sourceFile + "|@");
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
    private void deleteFileToTrash(File file) {
        this.osNative.deleteToTrash(file);
    }

}
