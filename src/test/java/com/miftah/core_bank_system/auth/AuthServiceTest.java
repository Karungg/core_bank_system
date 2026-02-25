package com.miftah.core_bank_system.auth;

import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.security.JwtService;
import com.miftah.core_bank_system.user.Role;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("miftah", "password123");
        loginRequest = new LoginRequest("miftah", "password123");

        user = User.builder()
                .id(UUID.randomUUID())
                .username("miftah")
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(registerRequest.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        UserDetails userDetails = mock(UserDetails.class);

        when(userDetailsService.loadUserByUsername(loginRequest.getUsername())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("mock-token");

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-token", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
