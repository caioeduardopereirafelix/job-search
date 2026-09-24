package br.com.jobsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@TestPropertySource(properties = "scheduler.enabled=false")
class ApplicationTests {

    @Test
    void contextLoads() {
    }

}
