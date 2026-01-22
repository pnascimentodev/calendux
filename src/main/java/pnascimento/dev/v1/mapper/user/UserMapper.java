package pnascimento.dev.v1.mapper.user;

import java.util.List;
import java.util.stream.Collectors;

import pnascimento.dev.v1.dto.plan.PlanDto;
import pnascimento.dev.v1.dto.plan.UserPlanDto;
import pnascimento.dev.v1.dto.user.UserCredentialsDto;
import pnascimento.dev.v1.dto.user.UserDto;
import pnascimento.dev.v1.dto.user.UserIdentityDto;

import pnascimento.dev.v1.entity.plan.PlanEntity;
import pnascimento.dev.v1.entity.plan.UserPlanEntity;
import pnascimento.dev.v1.entity.user.UserCredentialsEntity;
import pnascimento.dev.v1.entity.user.UserEntity;
import pnascimento.dev.v1.entity.user.UserIdentityEntity;


public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserDto.builder()
            .id(entity.getId())
            .fullName(entity.getFullName())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .identities(toIdentityDtoList(entity.getIdentities()))
            .plans(toUserPlanDtoList(entity.getPlans()))
            .currentPlan(toDto(entity.getCurrentPlan()))
            .build();
    }

    public static UserCredentialsDto toDto(UserCredentialsEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserCredentialsDto.builder()
            .userId(entity.getUserId())
            .passwordUpdatedAt(entity.getPasswordUpdatedAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public static UserIdentityDto toDto(UserIdentityEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserIdentityDto.builder()
            .id(entity.getId())
            .provider(entity.getProvider())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public static List<UserIdentityDto> toIdentityDtoList(List<UserIdentityEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
            .map(UserMapper::toDto)
            .collect(Collectors.toList());
    }

    public static UserPlanDto toDto(UserPlanEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserPlanDto.builder()
            .id(entity.getId())
            .status(entity.getStatus().name())
            .startsAt(entity.getStartsAt())
            .endsAt(entity.getEndsAt())
            .plan(toDto(entity.getPlan()))
            .build();
    }

    public static PlanDto toDto(PlanEntity entity) {
        if (entity == null) {
            return null;
        }

        return PlanDto.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .name(entity.getName())
            .description(entity.getDescription())
            .build();
    }

    public static List<UserPlanDto> toUserPlanDtoList(List<UserPlanEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
            .map(UserMapper::toDto)
            .collect(Collectors.toList());
    }
}
