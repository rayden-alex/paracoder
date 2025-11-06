package by.rayden.paracoder;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ParallelCoderApplication {

    static void main(String[] args) {
        try {
            // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000015340-Detecting-Intellij-from-within-main-methods?page=1#community_comment_360000017399
            // https://stackoverflow.com/questions/15339148/check-if-java-code-is-running-from-intellij-eclipse-etc-or-command-line
            Class.forName("com.intellij.rt.execution.application.AppMainV2");
            System.setProperty("jansi.passthrough", "true");
            // or add -Djansi.passthrough=true to VM options in the RunConfiguration
        } catch (ClassNotFoundException _) {
        }

        // Optionally remove existing handlers attached to j.u.l root logger
//        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
//        SLF4JBridgeHandler.install();

        ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(ParallelCoderApplication.class)
            .web(WebApplicationType.NONE)
            .bannerMode(Banner.Mode.CONSOLE)
            .logStartupInfo(false)
            .run(args);

        int exitCode = SpringApplication.exit(applicationContext);
        System.exit(exitCode);
    }

}
