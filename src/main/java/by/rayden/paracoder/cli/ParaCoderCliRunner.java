package by.rayden.paracoder.cli;

import by.rayden.paracoder.cli.command.CommandController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.jansi.graalvm.AnsiConsole;

import java.util.Arrays;
import java.util.Properties;

@Component
@Slf4j
public class ParaCoderCliRunner implements CommandLineRunner, ExitCodeGenerator {
    private final CommandController commandController;
    private final IFactory cliFactory; // auto-configured to inject PicocliSpringFactory
    private final UnicodeCommandLine unicodeCommandLine;

    private static final Properties staticProps = System.getProperties();

    private int exitCode;

    public ParaCoderCliRunner(CommandController commandController, IFactory cliFactory,
                              UnicodeCommandLine unicodeCommandLine) {
        this.commandController = commandController;
        this.cliFactory = cliFactory;
        this.unicodeCommandLine = unicodeCommandLine;
    }

    /**
     * Just in case, we destroy all child processes that could remain in progress
     */
    private static void createShutdownHook() {
        Thread shutdownHook = new Thread(ParaCoderCliRunner::destroyDescendantOnMainProcessExit);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void destroyDescendantOnMainProcessExit() {
        ProcessHandle.current().descendants()
                     .forEach(processHandle -> {
                         String processInfo = processHandle.info().commandLine()
                                                           .orElseGet(() -> "PID:" + processHandle.pid());
                         log.info("Descendant process to destroy: {}", processInfo);
                         processHandle.destroyForcibly();
                     });
    }

    /**
     * @see <a href="https://github.com/remkop/picocli-jansi-graalvm">picocli-jansi-graalvm</a>
     * @see <a href="https://picocli.info/#_ansi_colors_and_styles">picocli ansi_colors_and_styles</a>
     */
    @Override
    public void run(String... args) {
        log.info("ParaCoder started.");

        if (log.isDebugEnabled()) {
            staticProps.forEach((k, v) -> log.debug("Static system property {}={}", k, v));
            System.getProperties().forEach((k, v) -> log.debug("Runtime system property {}={}", k, v));
        }

        String[] unicodeArgs = this.unicodeCommandLine.getArguments(args);
        log.atInfo().setMessage("Unicode command line args: {}").addArgument(() -> Arrays.toString(unicodeArgs)).log();

        ParaCoderCliRunner.createShutdownHook();

        try (AnsiConsole _ = AnsiConsole.windowsInstall()) {
            // TODO: Log WARN PicocliSpringFactory - Unable to get bean of class interface java.util.List, using fallback factory
            this.exitCode = new CommandLine(this.commandController, this.cliFactory).execute(unicodeArgs);
        }

        log.info("ParaCoder completed.");
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }
}
