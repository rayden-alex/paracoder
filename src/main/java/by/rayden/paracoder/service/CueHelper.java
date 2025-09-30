package by.rayden.paracoder.service;

import by.rayden.paracoder.utils.OutUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.digitalmediaserver.cuelib.CueParser;
import org.digitalmediaserver.cuelib.CueSheet;
import org.digitalmediaserver.cuelib.Position;
import org.digitalmediaserver.cuelib.TrackData;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CueHelper {

    /**
     * Extension of CUE-files
     */
    public static final String CUE_EXT = "cue";

    private static final int SECONDS_PER_MINUTE = 60;
    private static final long NANOS_PER_SECOND = 1000_000_000L;
    private static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;

    /**
     * CD Audio (Red Book) has 75 frames per second
     */
    private static final int FRAMES_PER_SECOND = 75;
    private static final long NANOS_PER_FRAME = NANOS_PER_SECOND / FRAMES_PER_SECOND;

    /**
     * For the last track, there is no easy way to calculate its end time.
     * Therefore, we specify a fake value in advance that is greater than the possible real value.
     */
    private static final LocalTime END_OF_FILE_TIME = LocalTime.of(23, 59, 59);

    /**
     * If the source file has a BOM, then the corresponding charset will be used,
     * otherwise UTF-8 will be used.
     */
    public CueSheet readCueSheet(Path sourceFilePath) throws IOException {
        var bomInputStream = BOMInputStream.builder().setPath(sourceFilePath).get();
        Charset charset = Optional.ofNullable(bomInputStream.getBOM())
                                  .map(ByteOrderMark::getCharsetName)
                                  .map(Charset::forName)
                                  .orElse(StandardCharsets.UTF_8);

        return CueParser.parse(bomInputStream, charset);
    }

    @SneakyThrows
    public CueTrackPayload getCueTrackPayload(TrackData trackData, Path sourceFilePath) {
        var trackInterval = getTrackInterval(trackData);

        // It is assumed that the Audio file is located next to the source CUE file
        Path audioFilePath = sourceFilePath.resolveSibling(trackData.getParent().getFile());
        FileTime audioLastModifiedTime = Files.getLastModifiedTime(audioFilePath);
        CueSheet cueSheet = trackData.getParent().getParent();

        return CueTrackPayload
            .builder()
            .trackNumber(trackData.getNumber())
            .totalTracks(trackData.getParent().getTrackData().size())
            .title(trackData.getTitle())
            .performer(trackData.getPerformer() == null ? cueSheet.getPerformer() : trackData.getPerformer())
            .album(cueSheet.getTitle())
            .year(cueSheet.getYear() != -1 ? cueSheet.getYear() : null)
            .genre(cueSheet.getGenre())
            .comment(cueSheet.getComment())
            .discId(cueSheet.getDiscId())
            .discNumber(cueSheet.getDiscNumber() != -1 ? cueSheet.getDiscNumber() : null)
            .totalDiscs(cueSheet.getTotalDiscs() != -1 ? cueSheet.getTotalDiscs() : null)
            .startTime(trackInterval.start)
            .endTime(trackInterval.end)
            .sourceFilePath(sourceFilePath)
            .audioFilePath(audioFilePath)
            .audioFileTime(audioLastModifiedTime)
            .build();
    }

    /**
     * If necessary, it returns a filtered list, avoiding ambiguity - whether we want to recode the entire regular file
     * or split it according to the СUE-file.
     */
    public Map<Path, BasicFileAttributes> getFilteredPathMap(Map<Path, BasicFileAttributes> pathMap) {
        if (hasAmbiguousSetOfFiles(pathMap)) {
            OutUtils.ansiOut("@|yellow WARNING! The source files contain both CUE-files and regular files.|@");
            OutUtils.ansiOut("@|yellow This causes ambiguity during processing.|@");
            OutUtils.ansiOut("@|yellow Therefore, only CUE-files will be processed.|@");

            log.warn("The source files contain both CUE-files and regular files, only CUE-files will be processed.");

            return pathMap
                .entrySet().stream()
                .filter(this::isCueFileOrDirectory)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return pathMap;
    }

    private boolean hasAmbiguousSetOfFiles(Map<Path, BasicFileAttributes> pathMap) {
        Set<String> fileExtensionsSet = pathMap
            .entrySet().stream()
            .filter(entry -> entry.getValue().isRegularFile())
            .map(Map.Entry::getKey)
            .map(Path::toString)
            .map(FilenameUtils::getExtension)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        return fileExtensionsSet.size() > 1 && fileExtensionsSet.contains(CUE_EXT);
    }

    private boolean isCueFileOrDirectory(Map.Entry<Path, BasicFileAttributes> entry) {
        return entry.getValue().isDirectory()
            || CUE_EXT.equalsIgnoreCase(FilenameUtils.getExtension(entry.getKey().toString()));
    }

    private record TrackInterval(LocalTime start, LocalTime end) {
    }

    private TrackInterval getTrackInterval(TrackData trackData) {
        Position startPosition = trackData.getStartIndex().getPosition();
        LocalTime startTime = convertToTime(startPosition); // TODO: Do I need a PreGap processing ?

        LocalTime endTime;
        if (trackData == trackData.getParent().getTrackData().getLast()) {
            endTime = END_OF_FILE_TIME; // to the end of file
        } else {
            int nextTrackIndex = trackData.getNumber();
            TrackData nextTrackData = trackData.getParent().getTrackData().get(nextTrackIndex);
            endTime = convertToTime(nextTrackData.getStartIndex().getPosition());
        }
        return new TrackInterval(startTime, endTime);
    }

    private LocalTime convertToTime(@Nullable Position position) {
        Objects.requireNonNull(position);

        return LocalTime.ofNanoOfDay(position.getMinutes() * NANOS_PER_MINUTE
            + position.getSeconds() * NANOS_PER_SECOND
            + position.getFrames() * NANOS_PER_FRAME);
    }
}
