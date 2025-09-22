package by.rayden.paracoder.win32native;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OsNativeWindowsImplTest {

    @Test
    void removeFileToTrashTest() throws IOException {
        Path path = Files.createTempFile("paracoder", "test.tmp");
        assertThat(Files.exists(path));

        var osNative = new OsNativeWindowsImpl();
        osNative.deleteToTrash(path);

        assertThat(path.toFile()).doesNotExist();
    }
}
