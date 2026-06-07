package com.educore.dtocourse.mapper;


import com.educore.dtocourse.request.LessonMaterialCreateRequest;
import com.educore.dtocourse.request.LessonMaterialUpdateRequest;
import com.educore.dtocourse.response.LessonMaterialResponse;
import com.educore.lesson.Week;
import com.educore.lessonmaterial.LessonMaterial;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface LessonMaterialMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "weeks", ignore = true)
    @Mapping(target = "downloadCount", constant = "0")
    @Mapping(target = "youtubeVideoId", ignore = true)
    @Mapping(target = "lastDownloadedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "preview", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orderNumber", source = "orderNumber")
    LessonMaterial toEntity(LessonMaterialCreateRequest request);

    @Mapping(target = "weekIds", expression = "java(mapWeeksToIds(material.getWeeks()))")
    @Mapping(target = "embedUrl", expression = "java(material.getEmbedUrl())")
    @Mapping(target = "youtubeVideoId", source = "youtubeVideoId")
    LessonMaterialResponse toResponse(LessonMaterial material);

    default Set<Long> mapWeeksToIds(Set<Week> weeks) {
        if (weeks == null) return null;
        return weeks.stream().map(Week::getId).collect(Collectors.toSet());
    }

    @Mapping(target = "weeks", ignore = true)
    @Mapping(target = "youtubeVideoId", ignore = true)
    @Mapping(target = "lastDownloadedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "downloadCount", ignore = true)
    void updateEntityFromRequest(LessonMaterialUpdateRequest request, @MappingTarget LessonMaterial material);
}
