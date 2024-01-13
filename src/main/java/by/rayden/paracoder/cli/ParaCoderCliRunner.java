package by.rayden.paracoder.cli;

import by.rayden.paracoder.cli.command.ParaCoderMainCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.jansi.graalvm.AnsiConsole;

@Component
public class ParaCoderCliRunner implements CommandLineRunner, ExitCodeGenerator {
    private final ParaCoderMainCommand paraCoderMainCommand;
    private final IFactory cliFactory; // auto-configured to inject PicocliSpringFactory

    private int exitCode;

    public ParaCoderCliRunner(ParaCoderMainCommand paraCoderMainCommand, IFactory cliFactory) {
        this.paraCoderMainCommand = paraCoderMainCommand;
        this.cliFactory = cliFactory;
    }

    /**
     * @see <a href="https://github.com/remkop/picocli-jansi-graalvm">picocli-jansi-graalvm</a>
     * @see <a href="https://picocli.info/#_ansi_colors_and_styles">picocli ansi_colors_and_styles</a>
     */
    @Override
    public void run(String... args) {
        try (AnsiConsole ansi = AnsiConsole.windowsInstall()) {
            this.exitCode = new CommandLine(this.paraCoderMainCommand, this.cliFactory).execute(args);
        }
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }
}