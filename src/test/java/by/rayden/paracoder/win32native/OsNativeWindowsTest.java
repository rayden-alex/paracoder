package by.rayden.paracoder.win32native;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ParameterizedClass
@FieldSource("osNativeProvider")
class OsNativeWindowsTest {
    static List<OsNative> osNativeProvider = List.of(new OsNativeWindowsImpl(), new OsNativeWindowsFFM());

    @Parameter
    static OsNative osNative;

    @Test
    void removeFileToTrashTest() throws IOException {
        Path path = Files.createTempFile("paracoder", "test.tmp");
        assertThat(Files.exists(path));

        osNative.deleteToTrash(path);

        assertThat(path.toFile()).doesNotExist();
    }
}
