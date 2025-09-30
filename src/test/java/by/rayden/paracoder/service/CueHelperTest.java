package by.rayden.paracoder.service;

import org.assertj.core.data.TemporalUnitLessThanOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("MagicNumber")
@ExtendWith(MockitoExtension.class)
class CueHelperTest {

    @InjectMocks
    private CueHelper cueHelper;

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

        Map<Path, BasicFileAttributes> filteredPathMap = this.cueHelper.getFilteredPathMap(pathMap);

        assertThat(filteredPathMap).containsOnlyKeys(path2, path4, path5);
    }

    @Test
    void readCueSheetTest() throws Exception {
        Path sourceFilePath = Paths.get("src/test/resources/CyrillicUTF8.cue");
        var cueSheet = this.cueHelper.readCueSheet(sourceFilePath);

        assertThat(cueSheet.getGenre()).isEqualTo("Pop Rock");
        assertThat(cueSheet.getPerformer()).isEqualTo("Мара");
        assertThat(cueSheet.getAllTrackData()).hasSize(12);
        assertThat(cueSheet.getFileData().getFirst().getFile()).isEqualTo("Мара - 2013 - Почувствуй разницу.opus");
    }

    @Test
    void getCueTrackPayloadTest() throws IOException {
        Path sourceFilePath = Paths.get("src/test/resources/CyrillicUTF8.cue");
        var cueSheet = this.cueHelper.readCueSheet(sourceFilePath);
        var trackData = cueSheet.getFileData().getFirst().getTrackData().get(1); //Second track

        var cueTrackPayload = this.cueHelper.getCueTrackPayload(trackData, sourceFilePath);

        assertThat(cueTrackPayload).isNotNull();
        assertThat(cueTrackPayload.getPerformer()).isEqualTo("Мара");
        assertThat(cueTrackPayload.getTitle()).isEqualTo("Новое время");
        assertThat(cueTrackPayload.getGenre()).isEqualTo("Pop Rock");
        assertThat(cueTrackPayload.getDiscId()).isEqualTo("AE09C50C");
        assertThat(cueTrackPayload.getComment()).isEqualTo("ExactAudioCopy v1.0b3");
        assertThat(cueTrackPayload.getYear()).isEqualTo(2013);
        assertThat(cueTrackPayload.getTrackNumber()).isEqualTo(2);
        assertThat(cueTrackPayload.getTotalTracks()).isEqualTo(12);
        assertThat(cueTrackPayload.getAlbum()).isEqualTo("Почувствуй разницу");

        final var inaccuracy = new TemporalUnitLessThanOffset(1, ChronoUnit.MICROS);

        // INDEX 01 00:57:57  --->>  57sec 57frames. (75 frames per second)
        final int nanos = (int) ((57L * 1000_000_000L) / 75L);
        assertThat(cueTrackPayload.getStartTime()).isCloseTo(LocalTime.of(0, 0, 57, nanos), inaccuracy);
        // INDEX 01 03:49:00
        assertThat(cueTrackPayload.getEndTime()).isCloseTo(LocalTime.of(0, 3, 49, 0), inaccuracy);
    }
}
