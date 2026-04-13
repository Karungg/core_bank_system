package com.miftah.core_bank_system.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.miftah.core_bank_system.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class TransactionControllerTest {

    @Test
    void contextLoads() {
    }
}
