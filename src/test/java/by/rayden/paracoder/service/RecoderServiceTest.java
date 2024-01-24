package by.rayden.paracoder.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RecoderServiceTest {

    @BeforeAll
    public static void setupHeadlessMode() {
        // This is necessary to use AWT moveToTrash()
        System.setProperty("java.awt.headless", "false");
    }

    @Test
    void lastQuotedStringPatternTest() {
        Pattern lastQuotedStringPattern = Pattern.compile("\"(?<newFile>[^\"]+?)\"$");
        Matcher matcher = lastQuotedStringPattern.matcher("ww\"ttt\"dd\"aa\"");

        assertThat(matcher.find()).isTrue();
        String newFileName = matcher.group("newFile");

        assertThat(newFileName).isEqualTo("aa");
    }

    @Test
    @Disabled
    void removeFileToTrash() {
        Path path = Path.of("g:\\torrent\\Anggun - Luminescence\\Anggun - Luminescence.opus");
        Desktop.getDesktop().moveToTrash(path.toFile());
    }
}