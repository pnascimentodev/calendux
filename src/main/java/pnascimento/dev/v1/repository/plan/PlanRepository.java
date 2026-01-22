package pnascimento.dev.v1.repository.plan;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pnascimento.dev.v1.entity.plan.PlanEntity;


public interface PlanRepository extends JpaRepository<PlanEntity, Long> {

    Optional<PlanEntity> findByCode(String code);

    boolean existsByCode(String code);
}