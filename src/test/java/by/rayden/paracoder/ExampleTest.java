package by.rayden.paracoder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Add the following dependency to your build.gradle:
 *
 * <pre>
 * dependencies {
 *     // ...
 *     testImplementation "org.springframework.boot:spring-boot-starter-test:$springBootVersion"
 * }
 * </pre>
 */
@SpringBootTest(webEnvironment = NONE, classes = ParallelCoderApplication.class)
public class ExampleTest {

    @Autowired
    CommandLine.IFactory factory;

    @Autowired
    MyCommand myCommand;

    @Test
    public void testParsingCommandLineArgs() {
        CommandLine.ParseResult parseResult = new CommandLine(myCommand, factory)
            .parseArgs("-x", "abc", "sub", "-y", "123");
        assertEquals("abc", myCommand.x);
        assertNull(myCommand.positionals);

        assertTrue(parseResult.hasSubcommand());
        CommandLine.ParseResult subResult = parseResult.subcommand();
        MyCommand.Sub sub = (MyCommand.Sub) subResult.commandSpec().userObject();
        assertEquals("123", sub.y);
        assertNull(sub.positionals);
    }

    @Test
    public void testUsageHelp() {
        String expected = String.format("" +
            "Usage: mycommand [-hV] [-x=<x>] [<positionals>...] [COMMAND]%n" +
            "      [<positionals>...]   positional params%n" +
            "  -h, --help               Show this help message and exit.%n" +
            "  -V, --version            Print version information and exit.%n" +
            "  -x=<x>                   optional option%n" +
            "Commands:%n" +
            "  sub%n");
        String actual = new CommandLine(myCommand, factory).getUsageMessage(CommandLine.Help.Ansi.AUTO);
        assertEquals(expected, actual);
    }
}