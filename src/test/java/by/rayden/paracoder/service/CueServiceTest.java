package by.rayden.paracoder.service;

import org.assertj.core.data.TemporalUnitLessThanOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("MagicNumber")
@ExtendWith(MockitoExtension.class)
class CueServiceTest {

    @InjectMocks
    private CueService cueService;

    @Test
    void getFilteredPathMapTest() {
        BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
        when(fileAttributes.isRegularFile()).thenReturn(true);
        BasicFileAttributes dirAttributes = mock(BasicFileAttributes.class);
        when(dirAttributes.isDirectory()).thenReturn(true);

        Path path1 = Path.of("c:\\dir1\\file1.flac");
        Path path2 = Path.of("c:\\dir1\\file2.Cue");
        Path path3 = Path.of("c:\\dir2\\file3.wav");
        Path path4 = Path.of("c:\\dir2\\file4.cUe");
        Path path5 = Path.of("c:\\dir2\\");

        Map<Path, BasicFileAttributes> pathMap = Map.of(
            path1, fileAttributes,
            path2, fileAttributes,
            path3, fileAttributes,
            path4, fileAttributes,
            path5, dirAttributes
        );

        Map<Path, BasicFileAttributes> filteredPathMap = this.cueService.getFilteredPathMap(pathMap);

        assertThat(filteredPathMap).containsOnlyKeys(path2, path4, path5);
    }

    @Test
    void readCueSheetTest() throws Exception {
        Path sourceFilePath = Paths.get("src/test/resources/cue/CyrillicUTF8.cue");
        var cueSheet = this.cueService.readCueSheet(sourceFilePath);

        assertThat(cueSheet.getGenre()).isEqualTo("Pop Rock");
        assertThat(cueSheet.getPerformer()).isEqualTo("Мара");
        assertThat(cueSheet.getAllTrackData()).hasSize(12);
        assertThat(cueSheet.getFileData().getFirst().getFile()).isEqualTo("Мара - 2013 - Почувствуй разницу.opus");
    }

    @Test
    void readMultiFileCueSheetTest() throws Exception {
        Path sourceFilePath = Paths.get("src/test/resources/cue/MultiFileCue.cue");
        var cueSheet = this.cueService.readCueSheet(sourceFilePath);

        assertThat(cueSheet.getFileData()).hasSize(2);
        assertThat(cueSheet.getAllTrackData()).hasSize(7);

        assertThat(cueSheet.getFileData().getFirst().getFile()).isEqualTo("Side A.flac");
        assertThat(cueSheet.getFileData().getFirst().getTrackData()).hasSize(4);

        assertThat(cueSheet.getFileData().getLast().getFile()).isEqualTo("Side B.flac");
        assertThat(cueSheet.getFileData().getLast().getTrackData()).hasSize(3);
    }

    @Test
    void validateCueParseResult_ShouldThrowException_WhenWrongCueFormatFile() throws Exception {
        Path sourceFilePath = Paths.get("src/test/resources/cue/Stars In Stereo - Stars In Stereo.cue");
        var cueSheet = this.cueService.readCueSheet(sourceFilePath);

        assertThatRuntimeException()
            .isThrownBy(() -> this.cueService.validateCueParseResult(cueSheet))
            .withMessage("The source CUE file has an invalid format. No AUDIO file.");
    }

    @Test
    void readCueSheet_ShouldThrowException_WhenFileNotExists() {
        Path sourceFilePath = Paths.get("src/test/resources/cue/FileNotExistName.cue");

        assertThatExceptionOfType(NoSuchFileException.class)
            .isThrownBy(() -> this.cueService.readCueSheet(sourceFilePath));
    }

    @Test
    void getAllCueTracksPayloadListTest() throws IOException {
        Path sourceFilePath = Paths.get("src/test/resources/cue/CyrillicUTF8.cue");
        var cueTrackPayload = this.cueService.getAllCueTracksPayloadList(sourceFilePath).get(1); //Second track

        assertThat(cueTrackPayload).isNotNull();
        assertThat(cueTrackPayload.performer()).isEqualTo("Мара");
        assertThat(cueTrackPayload.title()).isEqualTo("Новое время");
        assertThat(cueTrackPayload.genre()).isEqualTo("Pop Rock");
        assertThat(cueTrackPayload.discId()).isEqualTo("AE09C50C");
        assertThat(cueTrackPayload.comment()).isEqualTo("ExactAudioCopy v1.0b3");
        assertThat(cueTrackPayload.year()).isEqualTo(2013);
        assertThat(cueTrackPayload.trackNumber()).isEqualTo(2);
        assertThat(cueTrackPayload.totalTracks()).isEqualTo(12);
        assertThat(cueTrackPayload.album()).isEqualTo("Почувствуй разницу");

        final var inaccuracy = new TemporalUnitLessThanOffset(1, ChronoUnit.MICROS);

        // INDEX 01 00:57:57  --->>  57sec 57frames. (75 frames per second)
        final int nanos = (int) ((57L * 1000_000_000L) / 75L);
        assertThat(cueTrackPayload.startTime()).isCloseTo(LocalTime.of(0, 0, 57, nanos), inaccuracy);
        // INDEX 01 03:49:00
        assertThat(cueTrackPayload.endTime()).isCloseTo(LocalTime.of(0, 3, 49, 0), inaccuracy);
    }

    @Test
    void getAllCueTracksPayloadList_ShouldThrowException_WhenNoTrackTitleFile() {
        Path sourceFilePath = Paths.get("src/test/resources/cue/Invalid Format No Title.cue");

        assertThatRuntimeException()
            .isThrownBy(() -> this.cueService.getAllCueTracksPayloadList(sourceFilePath))
            .withMessage("The source CUE file has an invalid format. No TRACK TITLE.");
    }

    @Test
    void getAllCueTracksPayloadList_ShouldThrowException_WhenAudioFileNotExists() {
        Path sourceFilePath = Paths.get("src/test/resources/cue/Invalid Format Audio File Not Exists.cue");

        assertThatRuntimeException()
            .isThrownBy(() -> this.cueService.getAllCueTracksPayloadList(sourceFilePath))
            .withCauseInstanceOf(NoSuchFileException.class)
            .withMessageContaining("Audio File Not Exists.wav");
    }

}
