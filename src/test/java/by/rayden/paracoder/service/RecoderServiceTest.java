package by.rayden.paracoder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RecoderServiceTest {

    @InjectMocks
    private RecoderService recoderService;

    @Test
    @SuppressWarnings("DuplicateExpressions")
    void reversedPathComparatorTest() {
        var paths = List.of(
            Path.of("d:\\level_1\\level_2\\Level_3"),
            Path.of("d:\\"),
            Path.of("d:\\level_1\\level_2"),
            Path.of("d:\\level_1\\"));

        var sortedPathList = new ArrayList<>(paths);
        sortedPathList.sort(RecoderService.REVERSED_PATH_COMPARATOR);

        var expectedPaths = List.of(
            Path.of("d:\\level_1\\level_2\\Level_3"),
            Path.of("d:\\level_1\\level_2"),
            Path.of("d:\\level_1\\"),
            Path.of("d:\\"));

        assertThat(sortedPathList).isEqualTo(expectedPaths);
    }

    @Test
    void lastQuotedStringPatternTest() {
        Matcher matcher = RecoderService.LAST_QUOTED_STRING_PATTERN.matcher("ww\"ttt\"dd\"aa\"");

        assertThat(matcher.find()).isTrue();
        String newFileName = matcher.group("targetFile");

        assertThat(newFileName).isEqualTo("aa");
    }

    @Test
    void readCueSheetTest() throws Exception {
        var cueSheet = this.recoderService.readCueSheet(Paths.get("src/test/resources/CyrillicUTF8.cue"));

        assertThat(cueSheet.getGenre()).isEqualTo("Pop Rock");
        assertThat(cueSheet.getPerformer()).isEqualTo("Мара");
    }
}
