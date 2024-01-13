package by.rayden.paracoder.service;

import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.nio.file.Path;

@Service
public class RecoderService {

    public int recode(final Iterable<Path> inputPathList) {
        try {
            inputPathList.forEach(inputPath -> {
                var str = CommandLine.Help.Ansi.ON.string(STR."Proceccing path ... : @|blue \{inputPath}|@");
//                AnsiConsole.out().println(str);
                System.out.println(str);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return CommandLine.ExitCode.OK;
    }
}
