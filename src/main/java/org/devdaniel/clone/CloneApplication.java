package org.devdaniel.clone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloneApplication.class, args);
    }

}
