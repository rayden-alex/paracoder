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

    private Path audioFilePath;

    @Nullable
    private String title;

    private LocalTime startTime;

    private LocalTime endTime;

    private File sourceFile;

    private FileTime audioFileTime;

}
