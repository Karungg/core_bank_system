package com.miftah.core_bank_system.auth;

import com.miftah.core_bank_system.exception.TokenRefreshException;
import com.miftah.core_bank_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    @Value("${application.security.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .token(UUID.randomUUID().toString())
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        
        if (token.isRevoked()) {
            log.warn("Attempt to use revoked refresh token for user: {}", token.getUser().getId());
            // Security measure: wipe all tokens if a revoked token is used
            deleteByUserId(token.getUser().getId());
            throw new TokenRefreshException(token.getToken(), "Refresh token is invalid or revoked. Please make a new signin request");
        }
        
        return token;
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        refreshTokenRepository.deleteByUser_Id(userId);
    }
    
    @Transactional
    public void deleteToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }
}
