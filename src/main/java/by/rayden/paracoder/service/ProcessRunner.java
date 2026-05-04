package by.rayden.paracoder.service;

import by.rayden.paracoder.utils.OutUtils;
import by.rayden.paracoder.win32native.OsNative;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProcessRunner {
    private final RecoderThreadPool pool;
    private final OsNative osNative;

    public ProcessRunner(@Lazy RecoderThreadPool pool, OsNative osNative) {
        this.pool = pool;
        this.osNative = osNative;
    }

    public CompletableFuture<Integer> execCommandAsync(String recodeCommand, Path sourceFilePath) {
        return CompletableFuture.supplyAsync(() -> exec(recodeCommand, sourceFilePath), this.pool.getExecutor());
    }

    // TODO 2024-02-07: Add process output redirect (configurable by CLI option) to a file.
    //  See java.lang.ProcessBuilder.Redirect.appendTo
    //  A file name should have a thread name to not mix output from different threads.

    // TODO 2024-02-07: Extract ProcessBuilder as class dependency for flexible testing.
    private int exec(String recodeCommand, Path sourceFilePath) {
        log.debug("Recode command: {}", recodeCommand);
        String threadName = Thread.currentThread().getName();
        OutUtils.ansiOut("Processing: @|yellow " + threadName + "|@ @|bold,blue " + sourceFilePath + "|@");

        try (Process lastProcess = runProcessWithRedirect(recodeCommand)) {
            boolean isCompleted = lastProcess.waitFor(9, TimeUnit.MINUTES);

            if (isCompleted) {
                return lastProcess.exitValue();
            } else {
                log.error("Waiting time for recode command has expired: {}", recodeCommand);
                throw new RuntimeException("Waiting time for recode command has expired: " + recodeCommand);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Recode command error: {}", recodeCommand, e);
            throw new RuntimeException(e);
        }
    }

    private Process runProcessWithRedirect(String recodeCommand) throws IOException {
        return runProcess(recodeCommand, true);
    }

    @VisibleForTesting
    Process runProcessWithoutRedirect(String recodeCommand) throws IOException {
        return runProcess(recodeCommand, false);
    }

    private Process runProcess(String recodeCommand, boolean useRedirects) throws IOException {
        List<ProcessBuilder> builders = makeProcessBuilders(recodeCommand);
        if (useRedirects) {
            applyRedirects(builders);
        }
        List<Process> processes = ProcessBuilder.startPipeline(builders);
        return processes.getLast();
    }

    /**
     * A recodeCommand can have multiple commands separated by | (a pipe character)
     * or it can have just one command/process to run as well.
     * <p>
     * If a pipe character is inside quotes then the entire string in quotes
     * is considered a single parameter and is not divided into two commands.
     * @see ProcessRunnerTest#testSplitCommandByPipeChar(String, String[])
     */
    @SuppressWarnings("JavadocReference")
    private List<ProcessBuilder> makeProcessBuilders(String recodeCommand) {
        return splitCommandByPipeChar(recodeCommand)
            .stream()
            .map(String::trim)
            .map(this::parseCommand)
            .map(ProcessBuilder::new)
            .toList();
    }

    /**
     * Splits the string by the | character, ignoring the ones inside quotes
     */
    @VisibleForTesting
    List<String> splitCommandByPipeChar(String command) {
        List<String> result = new ArrayList<>();

        int start = 0;
        boolean inQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == '|' && !inQuotes) {
                result.add(command.substring(start, i));
                start = i + 1;
            }
        }

        // If the flag remains true, it means that some quote is not closed.
        if (inQuotes) {
            throw new IllegalArgumentException("Command template error: odd number of quotes in the string.");
        }

        // Adding the last fragment
        result.add(command.substring(start));
        return result;
    }

    /**
     * The redirects for standard input of the first process
     * and standard output of the last process
     * are initialized using the redirect settings of the respective ProcessBuilder.
     * All other ProcessBuilder redirects should be Redirect.PIPE.
     */
    private void applyRedirects(List<ProcessBuilder> builderList) {
        builderList.getFirst()
                   .redirectInput(ProcessBuilder.Redirect.INHERIT)
                   .redirectError(ProcessBuilder.Redirect.INHERIT);

        builderList.getLast()
                   .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                   .redirectError(ProcessBuilder.Redirect.INHERIT);
    }

    private String[] parseCommand(String str) {
        log.debug("Command before parse: {}", str);
        String[] argv = this.osNative.getCommandLineToArgv(str);
        log.atDebug().setMessage("Command after parse: {}").addArgument(() -> Arrays.asList(argv)).log();
        return argv;
    }
}
