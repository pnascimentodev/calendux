package pnascimento.dev.v1.entity.user;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pnascimento.dev.v1.entity.plan.PlanEntity;
import pnascimento.dev.v1.entity.plan.UserPlanEntity;

@Entity
@Table(name = "tb_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "full_name", length = 160)
  private String fullName;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_plan_id")
  private PlanEntity currentPlan;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private UserCredentialsEntity credentials;

  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserIdentityEntity> identities = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserPlanEntity> plans = new ArrayList<>();
}