package by.rayden.paracoder.cli.command;

import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Component
@Command(name = "subsub", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 44)
public class SubSub implements Callable<Integer> {
    @Option(names = "-z", description = "optional option")
    public String z;

    @Override
    public Integer call() {
        System.out.printf("mycommand sub subsub was called with -z=%s. Service says: '%s'%n", this.z, 123);
        throw new RuntimeException("mycommand sub subsub failing on purpose");
        //return 43;
    }
}
