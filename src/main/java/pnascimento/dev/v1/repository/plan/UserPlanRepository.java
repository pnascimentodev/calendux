package pnascimento.dev.v1.repository.plan;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pnascimento.dev.v1.entity.plan.UserPlanEntity;
import pnascimento.dev.v1.entity.plan.UserPlanStatus;

public interface UserPlanRepository extends JpaRepository<UserPlanEntity, Long> {


    Optional<UserPlanEntity> findByUserIdAndStatus(Long userId, UserPlanStatus status);

}