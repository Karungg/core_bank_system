package com.miftah.core_bank_system.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.miftah.core_bank_system.audit.AuditAction;
import com.miftah.core_bank_system.audit.AuditService;
import com.miftah.core_bank_system.exception.AccountLockedException;
import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.exception.TokenRefreshException;
import com.miftah.core_bank_system.notification.event.LoginEvent;
import com.miftah.core_bank_system.security.JwtService;
import com.miftah.core_bank_system.user.Role;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.user.UserResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    @Mock
    private AuditService auditService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("miftah", "Pass@123");
        loginRequest = new LoginRequest("miftah", "Pass@123");

        user = User.builder()
                .id(UUID.randomUUID())
                .username("miftah")
                .password("encodedPassword")
                .role(Role.USER)
                .failedLoginAttempts(0)
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
        assertEquals(Role.USER, response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ShouldThrowDuplicateResourceException() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }



    @Test
    void login_Success_ShouldReturnTokenAndResetAttempts() {
        UserDetails userDetails = mock(UserDetails.class);

        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(loginRequest.getUsername())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("mock-token");
        when(refreshTokenService.createRefreshToken(user.getId()))
                .thenReturn(RefreshToken.builder().token("mock-refresh").build());

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-token", response.getToken());
        assertEquals("mock-refresh", response.getRefreshToken());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLoginLockedUntil());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(user);
        verify(auditService).logAction(eq(user), eq(AuditAction.LOGIN), anyString());
        verify(applicationEventPublisher).publishEvent(any(LoginEvent.class));
    }

    @Test
    void login_UserNotFound_ShouldThrowBadCredentialsException() {
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_AccountLocked_ShouldThrowBadCredentialsException() {
        user.setLoginLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_BadCredentials_ShouldIncrementFailedAttempts() {
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        assertEquals(1, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
        verify(applicationEventPublisher).publishEvent(any(LoginEvent.class));
    }

    @Test
    void login_FifthFailedAttempt_ShouldLockAccount() {
        user.setFailedLoginAttempts(4); // next failure is the 5th
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        assertNotNull(user.getLoginLockedUntil());
        assertEquals(0, user.getFailedLoginAttempts()); // reset on lock
        verify(userRepository).save(user);
    }

    @Test
    void login_ExpiredLock_ShouldProceedNormally() {
        // Lock expired 1 minute ago
        user.setLoginLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        UserDetails userDetails = mock(UserDetails.class);

        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(loginRequest.getUsername())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("mock-token");
        when(refreshTokenService.createRefreshToken(user.getId()))
                .thenReturn(RefreshToken.builder().token("mock-refresh").build());

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-token", response.getToken());
    }



    @Test
    void me_Success() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponse response = authService.me(user);

        assertNotNull(response);
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals(user.getId(), response.getId());
        assertEquals(user.getRole(), response.getRole());
    }

    @Test
    void me_UserNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.me(user));
        assertEquals("id", ex.getFieldName());
    }



    @Test
    void refreshToken_Success_ShouldRotateTokens() {
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

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(Optional.of(mockRefreshToken));
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
    void refreshToken_TokenNotFound_ShouldThrowTokenRefreshException() {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("invalid-refresh-token");
        when(refreshTokenService.findByToken("invalid-refresh-token")).thenReturn(Optional.empty());

        TokenRefreshException ex = assertThrows(TokenRefreshException.class,
                () -> authService.refreshToken(refreshRequest));
        assertEquals("invalid-refresh-token", ex.getToken());
    }



    @Test
    void logout_Success_ShouldDeleteTokenAndAudit() {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("valid-refresh-token");
        RefreshToken mockRefreshToken = RefreshToken.builder()
                .user(user)
                .token("valid-refresh-token")
                .build();

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(Optional.of(mockRefreshToken));

        authService.logout(refreshRequest);

        verify(refreshTokenService).deleteToken(mockRefreshToken);
        verify(auditService).logAction(eq(user), eq(AuditAction.LOGOUT), anyString());
    }

    @Test
    void logout_TokenNotFound_ShouldDoNothing() {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest("unknown-token");
        when(refreshTokenService.findByToken("unknown-token")).thenReturn(Optional.empty());

        authService.logout(refreshRequest);

        verify(refreshTokenService, never()).deleteToken(any());
        verify(auditService, never()).logAction(any(), any(), any());
    }
}
