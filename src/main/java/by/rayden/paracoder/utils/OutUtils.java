package by.rayden.paracoder.utils;

import lombok.experimental.UtilityClass;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

@UtilityClass
public class OutUtils {

    public void ansiOut(String str) {
        AnsiConsole.out().println(CommandLine.Help.Ansi.ON.string(str));
    }

    public void ansiErr(String str) {
        AnsiConsole.err().println(CommandLine.Help.Ansi.ON.string(str));
    }
}
