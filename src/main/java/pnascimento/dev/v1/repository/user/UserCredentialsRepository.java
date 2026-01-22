package pnascimento.dev.v1.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pnascimento.dev.v1.entity.user.UserCredentialsEntity;

public interface UserCredentialsRepository extends JpaRepository<UserCredentialsEntity, Long> {

    Optional<UserCredentialsEntity> findByUserId(Long userId);
}
