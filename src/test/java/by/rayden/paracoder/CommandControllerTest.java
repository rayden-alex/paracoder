package by.rayden.paracoder;

import by.rayden.paracoder.cli.command.CommandController;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import picocli.CommandLine;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static picocli.CommandLine.IFactory;

@SpringBootTest(args = {"--help"})
public class CommandControllerTest {
    @Autowired
    private IFactory cliFactory;

    @Autowired
    private CommandController commandController;

    @Test
    @Disabled
    public void testParsingCommandLineArgs() {
        int exitCode = new CommandLine(this.commandController, this.cliFactory)
            .execute("-dr c:\\temp");

        assertThat(exitCode).isZero();
        assertThat(this.commandController.getInputPathList()).isEmpty();
        assertThat(this.commandController.isRecurse()).isTrue();
        assertThat(this.commandController.isDeleteSourceFilesToTrash()).isTrue();
    }

    @Test
    public void testUsageHelp() {
        String actual =
            new CommandLine(this.commandController, this.cliFactory).getUsageMessage(CommandLine.Help.Ansi.OFF);

        assertThat(actual).contains(
            List.of("Usage: paracoder [-dhrV] [-pd] [-pf] [-t=<threadCount>] [<inputPathList>...]",
                "This is a ParaCoder application",
                "-h, --help                 Show this help message and exit",
                "-V, --version              Print version information and exit"));
    }
}
