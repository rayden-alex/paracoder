package by.rayden.paracoder.utils;

import lombok.experimental.UtilityClass;
import picocli.CommandLine;

@UtilityClass
public class OutUtils {

    public void ansiOut(String str) {
        System.out.println(CommandLine.Help.Ansi.ON.string(str));
    }

    public void ansiErr(String str) {
        System.err.println(CommandLine.Help.Ansi.ON.string(str));
    }
}
