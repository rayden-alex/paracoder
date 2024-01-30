package by.rayden.paracoder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties
@Slf4j
public class ParallelCoderApplication {

    public static void main(String[] args) {
        log.info("ParaCoder started.");
        try {
            // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000015340-Detecting-Intellij-from-within-main-methods?page=1#community_comment_360000017399
            // https://stackoverflow.com/questions/15339148/check-if-java-code-is-running-from-intellij-eclipse-etc-or-command-line
            Class.forName("com.intellij.rt.execution.application.AppMainV2");
            System.setProperty("jansi.passthrough", "true");
            // or add -Djansi.passthrough=true to VM options in the RunConfiguration
        } catch (ClassNotFoundException _) {
        }

        // Now I use OS native call to delete files to recycle bin (instead of Desktop.getDesktop().moveToTrash())
        // so no need to set "java.awt.headless" property to init AWT.
        ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(ParallelCoderApplication.class)
            .web(WebApplicationType.NONE)
            .headless(true)
            .bannerMode(Banner.Mode.CONSOLE)
            .logStartupInfo(false)
            .run(args);

        int exitCode = SpringApplication.exit(applicationContext);
        log.info("ParaCoder completed.");
        System.exit(exitCode);
    }

}
