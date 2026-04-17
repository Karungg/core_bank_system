package com.miftah.core_bank_system.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.miftah.core_bank_system.TestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class AccountGeneratorUtilTest {
    
    @Autowired
    private AccountGeneratorUtil accountGeneratorUtil;

    @Test
    void generateCvv() {
        String cvv = accountGeneratorUtil.generateCvv();
        assertNotNull(cvv);
        assertEquals(3, cvv.length());
    }

    @Test
    void generateAccountNumber() {
        String accountNumber = accountGeneratorUtil.generateAccountNumber();
        assertNotNull(accountNumber);
        assertEquals(10, accountNumber.length());
    }

    @Test
    void generateCardNumber() {
        String cardNumber = accountGeneratorUtil.generateCardNumber();
        assertNotNull(cardNumber);
        assertEquals(16, cardNumber.length());
    }

}
