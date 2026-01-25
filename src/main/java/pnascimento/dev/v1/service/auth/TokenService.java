package pnascimento.dev.v1.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pnascimento.dev.v1.entity.user.UserEntity;
import pnascimento.dev.v1.entity.user.UserTokenEntity;
import pnascimento.dev.v1.repository.user.UserTokenRepository;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserTokenRepository tokenRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new access token for the user
     */
    @Transactional
    public String generateAccessToken(UserEntity user) {
        String token = generateSecureToken();
        
        UserTokenEntity tokenEntity = UserTokenEntity.builder()
                .user(user)
                .token(token)
                .tokenType("ACCESS")
                .expiresAt(OffsetDateTime.now().plusHours(24)) // Token válido por 24 horas
                .isRevoked(false)
                .createdAt(OffsetDateTime.now())
                .build();

        tokenRepository.save(tokenEntity);
        return token;
    }

    /**
     * Generates a new refresh token for the user
     */
    @Transactional
    public String generateRefreshToken(UserEntity user) {
        String token = generateSecureToken();
        
        UserTokenEntity tokenEntity = UserTokenEntity.builder()
                .user(user)
                .token(token)
                .tokenType("REFRESH")
                .expiresAt(OffsetDateTime.now().plusDays(30)) // Token válido por 30 dias
                .isRevoked(false)
                .createdAt(OffsetDateTime.now())
                .build();

        tokenRepository.save(tokenEntity);
        return token;
    }

    /**
     * Revokes a specific token
     */
    @Transactional
    public void revokeToken(String token) {
        tokenRepository.revokeToken(token);
    }

    /**
     * Revokes all tokens for a specific user
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        tokenRepository.revokeAllUserTokens(userId);
    }

    /**
     * Validates if a token is valid (not revoked and not expired)
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return tokenRepository.findByTokenAndIsRevokedFalse(token)
                .map(t -> t.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElse(false);
    }

    /**
     * Generates a secure random token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
