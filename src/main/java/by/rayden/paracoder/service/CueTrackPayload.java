package by.rayden.paracoder.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;

/**
 * <a href="https://wiki.hydrogenaudio.org/index.php?title=Cue_sheet">Cue_sheet Wiki</a>
 */
@Getter
@Builder
public class CueTrackPayload {
    private int trackNumber;
    private int totalTracks;
    private String title;
    private String performer;

    @Nullable
    private String genre;

    @Nullable
    private Integer year;

    @Nullable
    private String album;

    @Nullable
    private String comment;

    @Nullable
    private String discId;

    @Nullable
    private Integer discNumber;

    @Nullable
    private Integer totalDiscs;

    private LocalTime startTime;
    private LocalTime endTime;

    private Path audioFilePath;
    private Path sourceFilePath;
    private FileTime audioFileTime;
}
