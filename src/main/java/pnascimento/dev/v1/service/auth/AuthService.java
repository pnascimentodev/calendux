package pnascimento.dev.v1.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public UserDto loginLocal(String email, String rawPassword) {
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

        return UserMapper.toDto(user);
    }

    /**
     * Method for logging in/registering via Google (OAuth2)
     */
    @Transactional
    public UserDto loginOrRegisterGoogle(String googleId, String email, String fullName) {
        // Check if this Google identity already exists
        return userIdentityRepository.findByProviderAndProviderUserId("GOOGLE", googleId)
                .map(identity -> UserMapper.toDto(identity.getUser()))
                .orElseGet(() -> {
                    // If identity doesn't exist, check if a user with this email already exists
                    UserEntity user = userRepository.findByEmail(email)
                            .orElseGet(() -> createNewUser(email, fullName));

                    // Link the Google identity to the user (new or existing)

                    addGoogleIdentity(user, googleId, email);

                    return UserMapper.toDto(userRepository.save(user));
                });
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