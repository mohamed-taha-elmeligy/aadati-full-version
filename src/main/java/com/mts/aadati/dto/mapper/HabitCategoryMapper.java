package com.mts.aadati.dto.mapper;

import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.dto.request.HabitCategoryRequest;
import com.mts.aadati.dto.response.HabitCategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitCategoryMapper {

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "habitCategoryId", ignore = true)
    HabitCategory toEntity(HabitCategoryRequest request);

    HabitCategoryResponse toResponse(HabitCategory habitCategory);

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "habitCategoryId", ignore = true)
    void update(HabitCategoryRequest request, @MappingTarget HabitCategory habitCategory);

    List<HabitCategoryRequest> toEntityList(List<HabitCategory> habitCategories);
}

