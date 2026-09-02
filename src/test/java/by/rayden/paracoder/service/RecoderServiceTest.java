package by.rayden.paracoder.service;

import by.rayden.paracoder.win32native.OsNative;
import by.rayden.paracoder.win32native.OsNativeWindowsFFM;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

@ExtendWith(MockitoExtension.class)
class RecoderServiceTest {

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
        // https://github.com/projectlombok/lombok/issues/4040
        // [BUG] SneakyThrows has a runtime dependency on lombok.Lombok on Java 26 #4040
        // Fixed in Lombok v1.18.48 (September 1st, 2026)
    void testDeleteToTrashFailsWhenFileIsLocked(@TempDir Path tempDir) throws Exception {
        // Create a temporary file
        File tempFile = tempDir.resolve("locked_file.txt").toFile();
        boolean _ = tempFile.createNewFile();
        var osNative = new OsNativeWindowsFFM();

        // Open a stream and exclusively lock the file
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             FileLock _ = fos.getChannel().lock()) { // The lock is held inside this block

            // Calling deleteToTrash() without @SneakyThrows throws an IOException
            assertThatIOException()
                .isThrownBy(() -> osNative.deleteToTrash(tempFile.toPath()))
                .withMessageContaining("ErrorCode=32")
                .withMessageContaining("ERROR_SHARING_VIOLATION");

            // Calling deleteToTrash() without @SneakyThrows but wrapped in RuntimeException throws RuntimeException
            assertThatRuntimeException()
                .isThrownBy(() -> deleteFileToTrashNoSneakyThrows(osNative, tempFile.toPath()))
                .withCauseInstanceOf(IOException.class);

//            // Calling deleteToTrash() with @SneakyThrows throws NoClassDefFoundError: lombok/Lombok
//            assertThatExceptionOfType(NoClassDefFoundError.class)
//                .isThrownBy(() -> deleteFileToTrashWithSneakyThrows(osNative, tempFile.toPath()))
//                .withMessageContaining("Lombok");

            // Calling deleteToTrash() with @SneakyThrows should throw an IOException
            assertThatIOException()
                .isThrownBy(() -> deleteFileToTrashWithSneakyThrows(osNative, tempFile.toPath()))
                .withMessageContaining("ErrorCode=32")
                .withMessageContaining("ERROR_SHARING_VIOLATION");
        }
    }

    @SneakyThrows
    private void deleteFileToTrashWithSneakyThrows(OsNative osNative, Path filePath) {
        osNative.deleteToTrash(filePath);
    }

    private void deleteFileToTrashNoSneakyThrows(OsNative osNative, Path filePath) {
        try {
            osNative.deleteToTrash(filePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
