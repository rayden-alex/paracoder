package by.rayden.paracoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ParallelCoderApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ParallelCoderApplication.class, args)));
    }

}
