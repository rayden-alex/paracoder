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
    header = "",
    description = CommandController.APP_DESCRIPTION,
    parameterListHeading = "%nParameters:%n",
    optionListHeading    = "%nOptions:%n",
    showDefaultValues = true,
    usageHelpAutoWidth = false,
    usageHelpWidth = 120,
    defaultValueProvider = CommandLine.PropertiesDefaultProvider.class,
    subcommands = Sub.class)
public class CommandController implements Callable<Integer> {
    static final String APP_DESCRIPTION = """
        
        ParaCoder CLI:
        This is a ParaCoder application
        to recode lossless audio files to different format using multiple threads.""";

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


    public CommandController(RecoderService recoderService) {
        this.recoderService = recoderService;
    }

    public List<Path> getInputPathList() {
        return Collections.unmodifiableList(this.inputPathList);
    }

    @Override
    public Integer call() {
        var paraCoderParams = new Params(getInputPathList(), this.preserveFileTimestamp, this.preserveDirTimestamp,
            this.recurse, this.deleteSourceFilesToTrash);

        return this.recoderService.recode(paraCoderParams);
    }

    public record Params(List<Path> inputPathList, boolean preserveFileTimestamp, boolean preserveDirTimestamp,
                         boolean recurse, boolean deleteSourceFilesToTrash) {
    }
}
