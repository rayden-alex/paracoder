package by.rayden.paracoder.service;

import org.springframework.stereotype.Service;
import picocli.CommandLine;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessFactory {
    private static final int N_THREADS = 4;
    private static final int WORK_QUEUE_CAPACITY = 100;
    private static final RejectedExecutionHandler REJECTED_HANDLER = new ThreadPoolExecutor.CallerRunsPolicy();
    private static final LinkedBlockingQueue<Runnable> WORK_QUEUE = new LinkedBlockingQueue<>(WORK_QUEUE_CAPACITY);
    private static final ExecutorService EXECUTOR_SERVICE = myFixedThreadPool(N_THREADS, threadFactory("cf-async-"));

    public CompletableFuture<Integer> execCommandAsync(String recodeCommand, Path sourceFilePath) {
        return CompletableFuture.supplyAsync(() -> exec(recodeCommand, sourceFilePath), EXECUTOR_SERVICE);
    }

    private int exec(String recodeCommand, Path sourceFilePath) {
        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command("cmd", "/c", recodeCommand);
        String threadName = Thread.currentThread().getName();
        System.out.println(CommandLine.Help.Ansi.ON.string(STR."Processing source file: @|yellow \{threadName}|@ @|blue \{sourceFilePath}|@"));
        try {
            Process process = processBuilder.start();
            boolean isCompleted = process.waitFor(9, TimeUnit.MINUTES);
            if (isCompleted) {
                return process.exitValue();
            } else {
                throw new RuntimeException(STR."Recode command is not completed: \{recodeCommand}");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static ThreadFactory threadFactory(String nameFormat) {
        return Thread.ofPlatform().name(nameFormat, 0).factory();
    }

    private static ExecutorService myFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
            WORK_QUEUE, threadFactory, REJECTED_HANDLER);
    }

    @PreDestroy
    public void stopMyPool() throws InterruptedException {
        EXECUTOR_SERVICE.shutdown();
        //noinspection ResultOfMethodCallIgnored
        EXECUTOR_SERVICE.awaitTermination(10, TimeUnit.SECONDS);
    }

}
