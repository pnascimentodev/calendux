package pnascimento.dev.v1.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pnascimento.dev.v1.dto.user.AuthResponseDto;
import pnascimento.dev.v1.dto.user.UserDto;
import pnascimento.dev.v1.entity.user.UserCredentialsEntity;
import pnascimento.dev.v1.entity.user.UserEntity;
import pnascimento.dev.v1.exception.AuthException;
import pnascimento.dev.v1.mapper.user.UserMapper;
import pnascimento.dev.v1.repository.user.UserRepository;
import pnascimento.dev.v1.repository.user.UserIdentityRepository;
import pnascimento.dev.v1.entity.user.UserIdentityEntity;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public UserDto registerLocal(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new AuthException.EmailAlreadyExists();
        }

        // Create user entity
        // Note: The database trigger will handle creating the record in tb_user_plans as FREE
        UserEntity user = UserEntity.builder()
                .email(email)
                .fullName(fullName)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Create credentials
        UserCredentialsEntity credentials = UserCredentialsEntity.builder()
                .user(user)
                .passwordHash(passwordEncoder.encode(password))
                .passwordUpdatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        user.setCredentials(credentials);

        UserEntity savedUser = userRepository.save(user);
        return UserMapper.toDto(savedUser);
    }

    @Transactional
    public AuthResponseDto loginLocal(String email, String rawPassword) {
        UserEntity user = userRepository.findByEmailFetchPlan(email)
                .orElseThrow(AuthException.InvalidCredentials::new);

        if (!user.getIsActive()) {
            throw new AuthException.AccountDisabled();
        }

        if (user.getCredentials() == null) {
            throw new RuntimeException("Usuário sem senha local. Utilize o login social.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getCredentials().getPasswordHash())) {
            throw new AuthException.InvalidCredentials();
        }

        // Gerar tokens de acesso
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        return AuthResponseDto.builder()
                .user(UserMapper.toDto(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public void logoutLocal(String token) {
        // Revogar o token específico
        tokenService.revokeToken(token);
    }

    @Transactional
    public void logoutAllSessions(Long userId) {
        // Revogar todos os tokens do usuário
        tokenService.revokeAllUserTokens(userId);
    }

    /**
     * Method for logging in/registering via Google (OAuth2)
     */
    @Transactional
    public AuthResponseDto loginOrRegisterGoogle(String googleId, String email, String fullName) {
        // Check if this Google identity already exists
        UserEntity user = userIdentityRepository.findByProviderAndProviderUserId("GOOGLE", googleId)
                .map(UserIdentityEntity::getUser)
                .orElseGet(() -> {
                    // If identity doesn't exist, check if a user with this email already exists
                    UserEntity existingUser = userRepository.findByEmail(email)
                            .orElseGet(() -> createNewUser(email, fullName));

                    // Link the Google identity to the user (new or existing)
                    addGoogleIdentity(existingUser, googleId, email);

                    return userRepository.save(existingUser);
                });

        // Gerar tokens de acesso
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        return AuthResponseDto.builder()
                .user(UserMapper.toDto(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    /**
     * Helper to create a base user
     */
    private UserEntity createNewUser(String email, String fullName) {
        return UserEntity.builder()
                .email(email)
                .fullName(fullName)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Helper to add Google provider to a user
     */
    private void addGoogleIdentity(UserEntity user, String googleId, String email) {
        UserIdentityEntity identity = UserIdentityEntity.builder()
                .user(user)
                .provider("GOOGLE")
                .providerUserId(googleId)
                .emailFromProvider(email)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        user.getIdentities().add(identity);
    }
}