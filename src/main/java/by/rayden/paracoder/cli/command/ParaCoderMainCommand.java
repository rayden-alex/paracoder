package by.rayden.paracoder.cli.command;

import by.rayden.paracoder.cli.PropertiesVersionProvider;
import by.rayden.paracoder.service.RecoderService;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

// NOTE: inner classes and fields are public for testing

@Component
@Command(name = "paracoder", versionProvider = PropertiesVersionProvider.class, mixinStandardHelpOptions = true,
    header = "ParaCoder CLI",
    description = "This is a ParaCoder application which recode lossless audio to another format using multiple " +
        "threads.",
    parameterListHeading = "%nParameters:%n",
    optionListHeading    = "%nOptions:%n",
    showDefaultValues = true,
    usageHelpAutoWidth = false,
    usageHelpWidth = 120,
    defaultValueProvider = CommandLine.PropertiesDefaultProvider.class,
    subcommands = Sub.class)
public class ParaCoderMainCommand implements Callable<Integer> {
    private final RecoderService recoderService;

    @Option(names = {"-pf", "--preserve-file-timestamp"},
        description = "Preserve original file timestamp (default: ${DEFAULT-VALUE}).")
    private boolean preserveFileTimestamp = true;

    @Option(names = {"-pd", "--preserve-dir-timestamp"},
        description = "Preserve original directories timestamp (default: ${DEFAULT-VALUE}).")
    private boolean preserveDirTimestamp = true;

    @Option(names = {"-r", "--recurse"},
//        defaultValue = "false",
//        showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
        description = "Recursively process all input directories (default: ${DEFAULT-VALUE}).")
    private boolean recurse = false;

    @Parameters(description = "Files and directories to recode")
    public List<Path> inputPathList;


    public ParaCoderMainCommand(RecoderService recoderService) {
        this.recoderService = recoderService;
    }

    @Override
    public Integer call() {
        System.out.println("picocli.defaults.paracoder.path="
            + System.getProperty("picocli.defaults.paracoder.path"));

        System.out.println("picocli.usage.width="
            + System.getProperty("picocli.usage.width"));

        if (this.inputPathList == null || this.inputPathList.isEmpty()) {
            return CommandLine.ExitCode.USAGE;
        }

        return this.recoderService.recode(this.inputPathList);
    }

}