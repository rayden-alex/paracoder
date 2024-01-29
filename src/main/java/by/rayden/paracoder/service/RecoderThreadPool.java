package by.rayden.paracoder.service;

import by.rayden.paracoder.cli.command.ParaCoderMainCommand;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RecoderThreadPool {
    private static final int WORK_QUEUE_CAPACITY = 100;
    private static final RejectedExecutionHandler REJECTED_HANDLER = new ThreadPoolExecutor.CallerRunsPolicy();
    private static final LinkedBlockingQueue<Runnable> WORK_QUEUE = new LinkedBlockingQueue<>(WORK_QUEUE_CAPACITY);

    private final ApplicationContext appCtx;

    @Getter
    private ExecutorService executor;


    public RecoderThreadPool(ApplicationContext appCtx) {
        this.appCtx = appCtx;
    }

    @SuppressWarnings("SameParameterValue")
    private ThreadFactory threadFactory(String nameFormat) {
        return Thread.ofPlatform().name(nameFormat, 0).factory();
    }

    //    @PostConstruct
    public void init() {
        ParaCoderMainCommand paraCoderMainCommand = this.appCtx.getBean(ParaCoderMainCommand.class);
        int threadCount = paraCoderMainCommand.getThreadCount();

        // TODO Make bean initialisation by Spring
        this.executor = new ThreadPoolExecutor(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS,
            WORK_QUEUE, threadFactory("cf-async-"), REJECTED_HANDLER);
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        if (this.executor != null) {
            this.executor.shutdown();
            //noinspection ResultOfMethodCallIgnored
            this.executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

}
