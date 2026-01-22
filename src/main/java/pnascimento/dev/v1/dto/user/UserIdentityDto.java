package pnascimento.dev.v1.dto.user;

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
public class UserIdentityDto {

    private Long id;
    private String provider;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}