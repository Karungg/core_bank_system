package com.miftah.core_bank_system.auth;

import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.security.JwtService;
import com.miftah.core_bank_system.user.Role;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.user.UserResponse;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.audit.AuditAction;
import com.miftah.core_bank_system.audit.AuditService;
import com.miftah.core_bank_system.notification.event.LoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import com.miftah.core_bank_system.exception.TokenRefreshException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final AuditService auditService;

    private final ApplicationEventPublisher applicationEventPublisher;

    public UserResponse register(RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new DuplicateResourceException("username", "error.username.duplicate");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        log.info("User registered successfully: {}", user.getId());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public TokenResponse login(LoginRequest request) {
        log.info("Logging in user: {}", request.getUsername());

        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());

        try {
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(Instant.now())) {
                    throw new BadCredentialsException("error.bad-credentials");
                }
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            User user = userOptional.orElseThrow(() -> new BadCredentialsException("error.bad-credentials"));

            user.setFailedLoginAttempts(0);
            user.setLoginLockedUntil(null);
            userRepository.save(user);

            log.info("User logged in successfully: {}", request.getUsername());
            auditService.logAction(user, AuditAction.LOGIN, "User logged in successfully");

            applicationEventPublisher.publishEvent(new LoginEvent(user.getId(), user.getUsername(), "unknown", true));

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String jwtToken = jwtService.generateToken(userDetails);
            
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            return TokenResponse.builder()
                    .token(jwtToken)
                    .refreshToken(refreshToken.getToken())
                    .build();

        } catch (AuthenticationException e) {
            userOptional.ifPresent(user -> {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setLoginLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
                    user.setFailedLoginAttempts(0);
                }
                userRepository.save(user);
                applicationEventPublisher.publishEvent(new LoginEvent(user.getId(), user.getUsername(), "unknown", false));
            });

            throw new BadCredentialsException("error.bad-credentials");
        }
    }

    @Transactional
    public TokenResponse refreshToken(TokenRefreshRequest request) {
        log.info("Processing refresh token request");
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(refreshToken -> {
                    User user = refreshToken.getUser();
                    
                    // Token rotation: delete the old one and generate a new one
                    refreshTokenService.deleteToken(refreshToken);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
                    
                    // Generate new JWT
                    String newJwt = jwtService.generateToken(user);
                    
                    log.info("Refresh token rotated successfully for user: {}", user.getUsername());
                    return TokenResponse.builder()
                        .token(newJwt)
                        .refreshToken(newRefreshToken.getToken())
                        .build();
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
    }

    @Transactional
    public void logout(TokenRefreshRequest request) {
        log.info("Processing logout request");
        String requestRefreshToken = request.getRefreshToken();
        
        refreshTokenService.findByToken(requestRefreshToken)
                .ifPresent(refreshToken -> {
                    User user = refreshToken.getUser();
                    refreshTokenService.deleteToken(refreshToken);
                    auditService.logAction(user, AuditAction.LOGOUT, "User logged out successfully");
                });
        
        log.info("Logout successful, refresh token deleted");
    }

    public UserResponse me(User currentUser) {
        log.info("Fetching current user details for: {}", currentUser.getUsername());
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
