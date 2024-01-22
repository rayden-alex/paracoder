package by.rayden.paracoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ParallelCoderApplication {

    public static void main(String[] args) {
        try {
            // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000015340-Detecting-Intellij-from-within-main-methods?page=1#community_comment_360000017399
            // https://stackoverflow.com/questions/15339148/check-if-java-code-is-running-from-intellij-eclipse-etc-or-command-line
            Class.forName("com.intellij.rt.execution.application.AppMainV2");
            System.setProperty("jansi.passthrough", "true");
            // or add -Djansi.passthrough=true to VM options in the RunConfiguration
        } catch (ClassNotFoundException _) {
        }

        int exitCode = SpringApplication.exit(SpringApplication.run(ParallelCoderApplication.class, args));
        System.exit(exitCode);
    }

}
