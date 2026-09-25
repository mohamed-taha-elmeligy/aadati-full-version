package com.mts.aadati.dto.mapper;

import com.mts.aadati.dto.request.RoleRequest;
import com.mts.aadati.dto.response.RoleResponse;
import com.mts.aadati.entities.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "users",ignore = true)
    Role toEntity(RoleRequest request);

    @Mapping(target = "isDeleted", source = "deleted")
    RoleResponse toResponse(Role role);

    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "users",ignore = true)
    void update(
            RoleRequest request,
            @MappingTarget Role role
    );

    List<RoleResponse> toResponseList(
            List<Role> roles
    );
}