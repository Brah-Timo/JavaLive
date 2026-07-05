package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * JavaLive Example Application entry point.
 *
 * <p>Demonstrates all JavaLive features in a real Spring Boot application.
 *
 * <p>To run:
 * <pre>
 * mvn spring-boot:run -pl javalive-example
 * </pre>
 *
 * <p>Then open: http://localhost:8080
 *
 * @author JavaLive Team
 */
@SpringBootApplication
@EnableScheduling   // needed for LiveSessionManager's idle cleanup
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
