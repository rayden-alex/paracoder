package by.rayden.paracoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ParallelCoderApplication {

    public static void main(String[] args) {
        int exitCode = SpringApplication.exit(SpringApplication.run(ParallelCoderApplication.class, args));
        System.exit(exitCode);
    }

}
