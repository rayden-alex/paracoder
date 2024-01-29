package by.rayden.paracoder.cli;

import by.rayden.paracoder.cli.command.ParaCoderMainCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.jansi.graalvm.AnsiConsole;

import java.util.Arrays;

@Component
@Slf4j
public class ParaCoderCliRunner implements CommandLineRunner, ExitCodeGenerator {
    private final ParaCoderMainCommand paraCoderMainCommand;
    private final IFactory cliFactory; // auto-configured to inject PicocliSpringFactory
    private final OsNative osNative;

    private int exitCode;

    public ParaCoderCliRunner(ParaCoderMainCommand paraCoderMainCommand, IFactory cliFactory, OsNative osNative) {
        this.paraCoderMainCommand = paraCoderMainCommand;
        this.cliFactory = cliFactory;
        this.osNative = osNative;
    }

    /**
     * @see <a href="https://github.com/remkop/picocli-jansi-graalvm">picocli-jansi-graalvm</a>
     * @see <a href="https://picocli.info/#_ansi_colors_and_styles">picocli ansi_colors_and_styles</a>
     */
    @Override
    public void run(String... args) {
        String[] fixedArgs = this.osNative.getCommandLineArguments(args);
        log.info("Unicode command line args: {}", Arrays.toString(fixedArgs));

        try (AnsiConsole ansi = AnsiConsole.windowsInstall()) {
            // TODO: Log WARN PicocliSpringFactory - Unable to get bean of class interface java.util.List, using fallback factory
            this.exitCode = new CommandLine(this.paraCoderMainCommand, this.cliFactory).execute(fixedArgs);
        }
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }
}