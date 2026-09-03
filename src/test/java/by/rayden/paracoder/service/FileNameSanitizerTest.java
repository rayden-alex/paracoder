package by.rayden.paracoder.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileNameSanitizerTest {

    private FileNameSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        this.sanitizer = new FileNameSanitizer();
    }

    @Test
    void testSanitize() {
        var fileNameOriginal = "file name with special symbols like ? : and * and < and \\ and 𐍈 and 😀";

        var result = this.sanitizer.sanitize(fileNameOriginal);

        // Forbidden characters in the file name must be replaced with Unicode equivalents,
        // while existing Unicode codePoints must be preserved unchanged.
        assertThat(result).isEqualTo("file name with special symbols like ？ ∶ and ∗ and ˂ and ⧹ and 𐍈 and 😀");
    }

}
