package by.rayden.paracoder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProcessFactory {
    private final RecoderThreadPool pool;

    public ProcessFactory(@Lazy RecoderThreadPool pool) {
        this.pool = pool;
    }

    public CompletableFuture<Integer> execCommandAsync(String recodeCommand, Path sourceFilePath) {
        return CompletableFuture.supplyAsync(() -> exec(recodeCommand, sourceFilePath), this.pool.getExecutor());
    }

    private int exec(String recodeCommand, Path sourceFilePath) {
        log.debug("Recode command: {}", recodeCommand);
        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command("cmd", "/d", "/c", recodeCommand);
        String threadName = Thread.currentThread().getName();
        System.out.println(CommandLine.Help.Ansi.ON.string(STR."Processing source file: @|yellow \{threadName}|@ @|blue \{sourceFilePath}|@"));
        try {
            Process process = processBuilder.start();
            boolean isCompleted = process.waitFor(9, TimeUnit.MINUTES);
            if (isCompleted) {
                return process.exitValue();
            } else {
                log.error("Waiting time for recode command has expired: {}", recodeCommand);
                throw new RuntimeException(STR."Waiting time for recode command has expired: \{recodeCommand}");
            }
        } catch (IOException | InterruptedException e) {
            log.error("Recode command error: {}", recodeCommand, e);
            throw new RuntimeException(e);
        }
    }

}
