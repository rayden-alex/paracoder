package by.rayden.paracoder.service;

import by.rayden.paracoder.win32native.OsNativeWindowsImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RecoderServiceTest {

    @Test
    void lastQuotedStringPatternTest() {
        Pattern lastQuotedStringPattern = Pattern.compile("\"(?<newFile>[^\"]+?)\"$");
        Matcher matcher = lastQuotedStringPattern.matcher("ww\"ttt\"dd\"aa\"");

        assertThat(matcher.find()).isTrue();
        String newFileName = matcher.group("newFile");

        assertThat(newFileName).isEqualTo("aa");
    }

    @Test
    void removeFileToTrash() throws IOException {
        Path path = Files.createTempFile("paracoder", "test.tmp");
        assertThat(path.toFile()).exists();

        var osNative = new OsNativeWindowsImpl();
        osNative.deleteToTrash(path.toFile());

        assertThat(path.toFile()).doesNotExist();
    }
}
