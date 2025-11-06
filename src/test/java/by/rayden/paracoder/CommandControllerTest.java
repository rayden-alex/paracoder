package by.rayden.paracoder;

import by.rayden.paracoder.cli.command.CommandController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static picocli.CommandLine.IFactory;

@SpringBootTest(webEnvironment = NONE, args = {"--help"})
public class CommandControllerTest {
    @Autowired
    private IFactory cliFactory;

    @Autowired
    private CommandController commandController;

    @Test
    public void testParsingCommandLineArgs() {
        new CommandLine(this.commandController, this.cliFactory)
            .parseArgs("--preserve-dir-timestamp", "-d", "temp.flac", "temp.cue");

        assertThat(this.commandController.isRecurse()).isFalse();
        assertThat(this.commandController.isPreserveFileTimestamp()).isTrue();
        assertThat(this.commandController.isDeleteSourceFilesToTrash()).isTrue();
        assertThat(this.commandController.getInputPathList())
            .containsExactlyInAnyOrder(Path.of("temp.flac"), Path.of("temp.cue"));
    }

    @Test
    public void testUsageHelp() {
        String actual = new CommandLine(this.commandController, this.cliFactory)
            .getUsageMessage(CommandLine.Help.Ansi.OFF);

        assertThat(actual).contains(
            List.of("Usage: paracoder [-dhrV] [-pd] [-pf] [-c=<configPath>] [-t=<threadCount>] <inputPathList>...",
                "This is a ParaCoder application",
                "-h, --help               Show this help message and exit.",
                "-V, --version            Print version information and exit."));
    }
}
