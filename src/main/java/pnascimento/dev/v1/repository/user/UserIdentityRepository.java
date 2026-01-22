package pnascimento.dev.v1.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import pnascimento.dev.v1.entity.user.UserIdentityEntity;

import java.util.List;
import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, Long> {

    Optional<UserIdentityEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<UserIdentityEntity> findAllByUserId(Long userId);
}
