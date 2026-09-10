package com.mts.aadati.dto.mapper;

import com.mts.aadati.entities.User;
import com.mts.aadati.dto.request.UserRequest;
import com.mts.aadati.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "habits", ignore = true)
    @Mapping(target = "habitTasks", ignore = true)
    @Mapping(target = "percentageDays", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toEntity(UserRequest request);

    UserResponse toResponse(User user);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "habits", ignore = true)
    @Mapping(target = "habitTasks", ignore = true)
    @Mapping(target = "percentageDays", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void update(UserRequest request, @MappingTarget User user);


    List<UserResponse> toResponseList(List<User> users);
}

