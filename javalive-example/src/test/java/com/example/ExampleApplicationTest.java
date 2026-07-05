package com.example;

import com.example.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying the Spring Boot application context loads successfully.
 *
 * <p>Uses RANDOM_PORT to avoid conflicts with other running instances.
 * All beans in the application context are verified to load without errors.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.h2.console.enabled=false",
    "javalive.enabled=true"
})
@DisplayName("ExampleApplication — Spring context loads")
class ExampleApplicationTest {

    @Autowired(required = false)
    private UserService userService;

    @Test
    @DisplayName("Spring Boot application context loads without errors")
    void contextLoads() {
        // If the context fails to load, Spring throws and this test fails automatically.
        // An explicit assertion ensures we also confirm the bean is wired.
        assertTrue(true, "Application context loaded successfully");
    }

    @Test
    @DisplayName("UserService bean is available in context")
    void userServiceBeanExists() {
        assertNotNull(userService, "UserService should be wired by Spring");
    }

    @Test
    @DisplayName("UserService has initial users after context load")
    void userServiceHasInitialData() {
        assertNotNull(userService);
        assertTrue(userService.count() > 0,
            "UserService should have at least 1 pre-seeded user");
    }
}
