package by.rayden.paracoder;

import by.rayden.paracoder.cli.command.ParaCoderMainCommand;
import by.rayden.paracoder.cli.command.Sub;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static picocli.CommandLine.Help;
import static picocli.CommandLine.IFactory;
import static picocli.CommandLine.ParseResult;

@SpringBootTest(webEnvironment = NONE, classes = ParallelCoderApplication.class)
@Disabled
public class ParaCoderMainCommandTest {
    @Autowired
    private IFactory cliFactory;

    @Autowired
    private ParaCoderMainCommand paraCoderMainCommand;

    @Test
    public void testParsingCommandLineArgs() {
        ParseResult parseResult = new CommandLine(this.paraCoderMainCommand, this.cliFactory)
            .parseArgs("-x", "abc", "sub", "-y", "123");

//        assertEquals("abc", this.paraCoderMainCommand.x);
        assertNull(this.paraCoderMainCommand.getInputPathList());

        assertTrue(parseResult.hasSubcommand());
        ParseResult subResult = parseResult.subcommand();
        Sub sub = (Sub) subResult.commandSpec().userObject();

        assertEquals("123", sub.y);
        assertNull(sub.positionals);
    }

    @Test
    public void testUsageHelp() {
        String expected = String.format(
            "Usage: paracoder [-hV] [-x=<x>] [<positionals>...] [COMMAND]%n" +
            "      [<positionals>...]   positional params%n" +
            "  -h, --help               Show this help message and exit.%n" +
            "  -V, --version            Print version information and exit.%n" +
            "  -x=<x>                   optional option%n" +
            "Commands:%n" +
            "  sub%n");

        String actual = new CommandLine(this.paraCoderMainCommand, this.cliFactory)
            .getUsageMessage(Help.Ansi.AUTO);
        assertEquals(expected, actual);
    }
}