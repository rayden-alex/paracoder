package by.rayden.paracoder.service;

import org.junit.jupiter.api.Test;

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
}