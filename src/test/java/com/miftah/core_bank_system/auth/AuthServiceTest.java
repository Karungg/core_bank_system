package com.miftah.core_bank_system.auth;

import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.security.JwtService;
import com.miftah.core_bank_system.user.Role;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.user.UserResponse;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.exception.TokenRefreshException;
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
    @Mock
    private RefreshTokenService refreshTokenService;

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
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(java.util.Optional.of(user));
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(RefreshToken.builder().token("mock-refresh").build());

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-token", response.getToken());
        assertEquals("mock-refresh", response.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void me_Success() {
        when(userRepository.findById(user.getId())).thenReturn(java.util.Optional.of(user));

        UserResponse response = authService.me(user);

        assertNotNull(response);
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals(user.getId(), response.getId());
    }

    @Test
    void me_UserNotFound_ThrowsException() {
        when(userRepository.findById(user.getId())).thenReturn(java.util.Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> authService.me(user));
        assertEquals("id", exception.getFieldName());
    }

    @Test
    void refreshToken_Success() {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("valid-refresh-token");
        RefreshToken mockRefreshToken = RefreshToken.builder()
                .user(user)
                .token("valid-refresh-token")
                .expiryDate(Instant.now().plusMillis(10000))
                .build();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token("new-refresh-token")
                .expiryDate(Instant.now().plusMillis(10000))
                .build();

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(java.util.Optional.of(mockRefreshToken));
        when(refreshTokenService.verifyExpiration(mockRefreshToken)).thenReturn(mockRefreshToken);
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(newRefreshToken);
        when(jwtService.generateToken(user)).thenReturn("new-jwt-token");

        TokenResponse response = authService.refreshToken(refreshRequest);

        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        
        verify(refreshTokenService).deleteToken(mockRefreshToken);
        verify(refreshTokenService).createRefreshToken(user.getId());
    }

    @Test
    void refreshToken_TokenNotFound_ThrowsException() {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("invalid-refresh-token");
        when(refreshTokenService.findByToken("invalid-refresh-token")).thenReturn(java.util.Optional.empty());

        TokenRefreshException exception = assertThrows(TokenRefreshException.class, () -> authService.refreshToken(refreshRequest));
        assertEquals("invalid-refresh-token", exception.getToken());
    }
}
