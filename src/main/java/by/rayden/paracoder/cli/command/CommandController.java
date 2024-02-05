package by.rayden.paracoder.cli.command;

import by.rayden.paracoder.cli.PropertiesVersionProvider;
import by.rayden.paracoder.service.RecoderService;
import lombok.Getter;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "paracoder",
    versionProvider = PropertiesVersionProvider.class,
    mixinStandardHelpOptions = true,
    header = "ParaCoder CLI",
    description = """
        This is a ParaCoder application
        to recode lossless audio files to another format using multiple threads.""",
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
    @Getter
    private boolean preserveFileTimestamp = true;

    @Option(names = {"-pd", "--preserve-dir-timestamp"},
        description = "Preserve original directories timestamp (default: ${DEFAULT-VALUE}).")
    @Getter
    private boolean preserveDirTimestamp = true;

    @Option(names = {"-r", "--recurse"},
        description = "Recursively process all input directories (default: ${DEFAULT-VALUE}).")
    @Getter
    private boolean recurse = false;

    @Option(names = {"-d", "--delete-to-trash"},
        description = "Delete source files to the trash (default: ${DEFAULT-VALUE}).")
    @Getter
    private boolean deleteSourceFilesToTrash = false;

    @Option(names = {"-t", "--thread-count"},
        showDefaultValue = CommandLine.Help.Visibility.NEVER,
        description = "The number of threads to use for recode (default: ${DEFAULT-VALUE}).")
    @Getter
    private int threadCount = 4;

    @Parameters(description = "Files and directories to recode")
    private List<Path> inputPathList;


    public ParaCoderMainCommand(RecoderService recoderService) {
        this.recoderService = recoderService;
    }

    public List<Path> getInputPathList() {
        return Collections.unmodifiableList(this.inputPathList);
    }

    @Override
    public Integer call() {
//        System.out.println("picocli.defaults.paracoder.path="
//            + System.getProperty("picocli.defaults.paracoder.path"));
//
//        System.out.println("picocli.usage.width="
//            + System.getProperty("picocli.usage.width"));

        if (this.inputPathList == null || this.inputPathList.isEmpty()) {
            return CommandLine.ExitCode.USAGE;
        }

        var paraCoderParams = new Params(this.inputPathList, this.preserveFileTimestamp, this.preserveDirTimestamp,
            this.recurse, this.deleteSourceFilesToTrash);

        return this.recoderService.recode(paraCoderParams);
    }

    public record Params(List<Path> inputPathList, boolean preserveFileTimestamp, boolean preserveDirTimestamp,
                         boolean recurse, boolean deleteSourceFilesToTrash) {
    }
}
