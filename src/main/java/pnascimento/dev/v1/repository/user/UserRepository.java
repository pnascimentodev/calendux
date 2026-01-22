package pnascimento.dev.v1.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pnascimento.dev.v1.entity.user.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.currentPlan WHERE u.email = :email")
    Optional<UserEntity> findByEmailFetchPlan(@Param("email") String email);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailAndIsActiveTrue(String email);

    Optional<UserEntity> findByIdAndIsActiveTrue(Long id);

    boolean existsByEmail(String email);
}
