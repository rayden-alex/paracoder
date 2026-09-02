package by.rayden.paracoder.service;

import lombok.Builder;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;

/**
 * <a href="https://wiki.hydrogenaudio.org/index.php?title=Cue_sheet">Cue_sheet Wiki</a>
 */
@Builder
public record CueTrackPayload(
    int trackNumber,
    int totalTracks,
    String title,
    String performer,

    @Nullable String genre,
    @Nullable Integer year,
    @Nullable String album,
    @Nullable String comment,
    @Nullable String discId,
    @Nullable Integer discNumber,
    @Nullable Integer totalDiscs,

    LocalTime startTime,
    LocalTime endTime,

    Path audioFilePath,
    Path sourceFilePath,
    FileTime audioFileTime
) {}
