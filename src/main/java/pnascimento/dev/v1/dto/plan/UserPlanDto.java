package pnascimento.dev.v1.dto.plan;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlanDto {

    private Long id;
    private String status;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private PlanDto plan;
}