package by.rayden.paracoder.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;

@Getter
@Builder
public class CueTrackPayload {
    private int songNumber;

    @Nullable
    private Path audioFilePath;

    @Nullable
    private String title;

    @Nullable
    private LocalTime startTime;

    @Nullable
    private LocalTime endTime;

    private File sourceFile;

    private FileTime sourceFileTime;

}
