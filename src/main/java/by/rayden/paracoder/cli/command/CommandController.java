package by.rayden.paracoder.cli.command;

import by.rayden.paracoder.cli.ParacoderParamsReadyEvent;
import by.rayden.paracoder.cli.PropertiesVersionProvider;
import by.rayden.paracoder.config.PatternProperties;
import by.rayden.paracoder.service.RecoderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@RequiredArgsConstructor
@Command(name = "paracoder",
    versionProvider = PropertiesVersionProvider.class,
    mixinStandardHelpOptions = true,
    header = "",
    description = CommandController.APP_DESCRIPTION,
    parameterListHeading = "%nParameters:%n",
    optionListHeading    = "%nOptions:%n",
    usageHelpWidth = 120
)
public class CommandController implements Callable<Integer> {
    static final String APP_DESCRIPTION = """
        
        ParaCoder CLI:
        This is a ParaCoder application
        to recode files/directories to different format using multiple threads.""";

    private final ApplicationEventPublisher eventPublisher;
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
        description = "The number of threads to use for recode (default: ${DEFAULT-VALUE}).")
    @Getter
    private int threadCount = 4;

    /**
     * @see PatternProperties
     */
    @Option(names = {"-c", "--config-location"},
        description = "Location of the main configuration YAML file (default: ${DEFAULT-VALUE}).")
    @Getter
    private Path configPath = Path.of("paracoder_commands.yml");

    @Parameters(arity = "1..*", description = "Files and directories to recode")
    private List<Path> inputPathList;


    public List<Path> getInputPathList() {
        return this.inputPathList == null ? Collections.emptyList() : Collections.unmodifiableList(this.inputPathList);
    }

    @Override
    public Integer call() {
        this.eventPublisher.publishEvent(new ParacoderParamsReadyEvent(this));

        var paraCoderParams = new Params(getInputPathList(), this.preserveFileTimestamp, this.preserveDirTimestamp,
            this.recurse, this.deleteSourceFilesToTrash);

        return this.recoderService.recode(paraCoderParams);
    }

    public record Params(List<Path> inputPathList, boolean preserveFileTimestamp, boolean preserveDirTimestamp,
                         boolean recurse, boolean deleteSourceFilesToTrash) {
    }
}
