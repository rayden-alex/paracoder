package by.rayden.paracoder.service;

import by.rayden.paracoder.win32native.OsNativeWindowsImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

class RecoderServiceTest {

    @Test
    @SuppressWarnings("DuplicateExpressions")
    void ReversedPathComparatorTest() {
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
    @SneakyThrows
    void removeFileToTrash() {
        Path path = Files.createTempFile("paracoder", "test.tmp");
        assertThat(path.toFile()).exists();

        var osNative = new OsNativeWindowsImpl();
        osNative.deleteToTrash(path.toFile());

        assertThat(path.toFile()).doesNotExist();
    }
}
