package pnascimento.dev.v1.dto.user;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pnascimento.dev.v1.dto.plan.UserPlanDto;
import pnascimento.dev.v1.dto.plan.PlanDto;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String fullName;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private List<UserIdentityDto> identities;
    private List<UserPlanDto> plans;
    private PlanDto currentPlan;
}
