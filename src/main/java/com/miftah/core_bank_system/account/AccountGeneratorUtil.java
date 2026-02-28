package com.miftah.core_bank_system.account;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class AccountGeneratorUtil {
    
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCvv()
    {
        return String.format("%03d", secureRandom.nextInt(1000));
    }

    public String generateAccountNumber()
    {
        StringBuilder accountNumber = new StringBuilder();

        accountNumber.append(secureRandom.nextInt(9) + 1);

        for (int i=0; i < 9; i++) {
            accountNumber.append(secureRandom.nextInt(9));
        }

        return accountNumber.toString();
    }

    public String generateCardNumber()
    {
        StringBuilder cardNumber = new StringBuilder();

        cardNumber.append(secureRandom.nextInt(9) + 1);

        for (int i=0; i < 15; i++) {
            cardNumber.append(secureRandom.nextInt(9));
        }

        return cardNumber.toString();
    }
}