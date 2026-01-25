package pnascimento.dev.v1.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pnascimento.dev.v1.entity.user.UserTokenEntity;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserTokenEntity, Long> {

    Optional<UserTokenEntity> findByTokenAndIsRevokedFalse(String token);

    @Modifying
    @Query("UPDATE UserTokenEntity t SET t.isRevoked = true, t.revokedAt = CURRENT_TIMESTAMP WHERE t.user.id = :userId AND t.isRevoked = false")
    int revokeAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserTokenEntity t SET t.isRevoked = true, t.revokedAt = CURRENT_TIMESTAMP WHERE t.token = :token AND t.isRevoked = false")
    int revokeToken(@Param("token") String token);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM UserTokenEntity t WHERE t.token = :token AND t.isRevoked = false")
    boolean isTokenValid(@Param("token") String token);
}
