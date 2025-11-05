package by.rayden.paracoder.config;

import by.rayden.paracoder.cli.ParacoderParamsReadyEvent;
import by.rayden.paracoder.cli.command.CommandController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE, args = {"--version"})
class PatternPropertiesTest {
    @Autowired
    private PatternProperties patternProperties;

    @Test
    void patternProperties_onParamsReadyEvent() {
        var commandController = mock(CommandController.class);
        when(commandController.getConfigPath()).thenReturn(Path.of("src/test/resources/paracoder_commands.yml"));

        this.patternProperties.onParamsReadyEvent(new ParacoderParamsReadyEvent(commandController));

        assertThat(this.patternProperties).isNotNull();
        assertThat(this.patternProperties.getCommandTemplate()).isNotEmpty();
        assertThat(this.patternProperties.getFileExtensions()).isNotEmpty();
    }

}
