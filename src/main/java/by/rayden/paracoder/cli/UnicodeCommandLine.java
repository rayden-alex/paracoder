package by.rayden.paracoder.cli;

import by.rayden.paracoder.ParallelCoderApplication;
import by.rayden.paracoder.win32native.OsNative;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class UnicodeCommandLine {
    private final OsNative osNative;

    public UnicodeCommandLine(OsNative osNative) {
        this.osNative = osNative;
    }

    /**
     * This method solves the issue when java executable cannot transfer argument in UTF-8 encoding.
     * Unicode symbols screws up and application receives ??????? instead of real text.
     */
    public String[] getArguments(String[] fallBackTo) {
        try {
            log.debug("In case we fail fallback would happen to: {}", Arrays.toString(fallBackTo));
            String[] commandLine = this.osNative.getUnicodeCommandLine();
            log.debug("According to Windows API program was started with arguments: {}", Arrays.toString(commandLine));

            List<String> argumentsOnly = getArgumentsOnly(commandLine);
            log.debug("argumentsOnly={}", argumentsOnly);

            String[] argumentsArray = (!argumentsOnly.isEmpty()) ? argumentsOnly.toArray(String[]::new) : fallBackTo;
            log.debug("These arguments will be used: {}", Arrays.toString(argumentsArray));
            return argumentsArray;
        } catch (Throwable t) {
            log.error("Failed to use JNA to get current program command line arguments", t);
            return fallBackTo;
        }
    }

    @VisibleForTesting
    protected List<String> getArgumentsOnly(String[] commandLine) {
        final String appMainClassName = ParallelCoderApplication.class.getName();

        boolean isArgsStartFound = false;
        List<String> argumentsOnly = new ArrayList<>();

        for (int i = 0; i < commandLine.length; i++) {
            String param = commandLine[i];

            if (isArgsStartFound) {
                argumentsOnly.add(param);

            } else if ("-jar".equalsIgnoreCase(param)
                && ((i + 1) < commandLine.length)
                && StringUtils.endsWithIgnoreCase(commandLine[i + 1], ".jar")) {
                // program args after jar file name
                isArgsStartFound = true;
                i++;

            } else if (param.equals(appMainClassName)) {
                // program args after main class name
                isArgsStartFound = true;
            }
        }
        return argumentsOnly;
    }

    @VisibleForTesting
    protected String getJarFileName(String codeSourcePath) {
        return StringUtils.endsWithIgnoreCase(codeSourcePath, ".jar")
            ? Path.of(codeSourcePath).getFileName().toString() : "";
    }

    /**
     * Warning! Not working for me.
     * @return JarPath or empty string when App running NOT from jar but from pure class.
     * @throws URISyntaxException if URL cannot be converted to a URI.
     */
    @VisibleForTesting
    protected String getCodeSourcePath() throws URISyntaxException {
        return getClass()
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .toURI()
            .getPath();
    }

}
