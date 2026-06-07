package com.educore.dtocourse.mapper;

import com.educore.dtocourse.request.LessonUpdateRequest;
import com.educore.dtocourse.request.WeekCreateRequest;
import com.educore.dtocourse.response.LessonMaterialResponse;
import com.educore.dtocourse.response.WeekResponse;
import com.educore.dtocourse.response.WeekSummaryResponse;
import com.educore.lesson.Week;
import com.educore.lessonmaterial.LessonMaterial;
import com.educore.unit.Session;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = LessonMaterialMapper.class)
public interface WeekMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessions", ignore = true)
    @Mapping(target = "materials", ignore = true)
    @Mapping(target = "quizzes", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "globallyLocked", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hasQuiz", ignore = true)
    Week toEntity(WeekCreateRequest request);

    @Mapping(target = "sessionIds", expression = "java(mapSessionsToIds(week.getSessions()))")
    @Mapping(target = "materials", source = "materials")
    @Mapping(target = "requiresSequentialAccess", source = "requiresSequentialAccess")
    WeekResponse toResponse(Week week);

    default Set<Long> mapSessionsToIds(Set<Session> sessions) {
        if (sessions == null) return null;
        return sessions.stream().map(Session::getId).collect(Collectors.toSet());
    }


    @Mapping(target = "hasQuiz", source = "hasQuiz")
    WeekSummaryResponse toSummaryResponse(Week week);

    List<WeekSummaryResponse> toSummaryList(List<Week> weeks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessions", ignore = true)
    @Mapping(target = "materials", ignore = true)
    @Mapping(target = "quizzes", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "globallyLocked", ignore = true)
    @Mapping(target = "requiresSequentialAccess", source = "requiresSequentialAccess")
    @Mapping(target = "lockType", ignore = true)
    @Mapping(target = "lockAfterDays", ignore = true)
    @Mapping(target = "lockDate", ignore = true)
    @Mapping(target = "hasQuiz", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(LessonUpdateRequest request, @MappingTarget Week week);
}
