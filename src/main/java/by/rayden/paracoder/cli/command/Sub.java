package by.rayden.paracoder.cli.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

@Component
@Command(name = "sub", mixinStandardHelpOptions = true, subcommands = SubSub.class, exitCodeOnExecutionException = 34)
public class Sub implements Callable<Integer> {
    @Option(names = "-y", description = "optional option")
    public String y;

    @Parameters(description = "positional params")
    public List<String> positionals;

    @Override
    public Integer call() {
        System.out.printf("mycommand sub was called with -y=%s and positionals: %s%n", this.y, this.positionals);
        throw new RuntimeException("mycommand sub failing on purpose");
        //return 33;
    }
}
