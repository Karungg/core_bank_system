package com.miftah.core_bank_system.auth;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        registerRequest = new RegisterRequest("testuser", "password123");
    }

    @Test
    void register_ShouldPersistUser() {
        UserResponse response = authService.register(registerRequest);

        assertNotNull(response.getId());
        assertEquals("testuser", response.getUsername());
        assertTrue(userRepository.existsByUsername("testuser"));
    }

    @Test
    void register_DuplicateUsername_ShouldThrowException() {
        authService.register(registerRequest);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
    }

    @Test
    void login_Success_ShouldReturnToken() {
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        TokenResponse tokenResponse = authService.login(loginRequest);

        assertNotNull(tokenResponse.getToken());
    }

    @Test
    void login_InvalidPassword_ShouldThrowException() {
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}
