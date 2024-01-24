package by.rayden.paracoder;

import by.rayden.paracoder.config.PatternProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
public class PatternPropertiesTest {
    @Autowired
    private PatternProperties patternProperties;

    @Test
    public void testPatternProperties() {
        assertThat(this.patternProperties.getFileExtensions()).isNotEmpty();
        assertThat(this.patternProperties.getCommandTemplate().keySet()).isNotEmpty();
        assertThat(this.patternProperties.getCommandTemplate().values()).isNotEmpty();
    }

}
