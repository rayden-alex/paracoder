package by.rayden.paracoder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        Map<Path, BasicFileAttributes> filteredPathMap = this.recoderService.getFilteredPathMap(pathMap);

        assertThat(filteredPathMap).containsOnlyKeys(path2, path4, path5);
    }
}
