package com.educore.dtocopon;

import com.educore.copon.AccessCode;
import com.educore.unit.Session;
import com.educore.copon.AccessCodeUsage;
import com.educore.copon.CodeTargetType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AccessCodeMapper {

    @Mapping(target = "targetType", expression = "java(ac.getTargetType().name())")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.title")
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "sessionTitle", source = "session.title")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "remainingUses", expression = "java(ac.getRemainingUses())")
    AccessCodeDto toDto(AccessCode ac);

    List<AccessCodeDto> toDtoList(List<AccessCode> codes);

    @Mapping(target = "targetType", expression = "java(ac.getTargetType().name())")
    @Mapping(target = "targetName", expression = "java(getTargetName(ac))")
    @Mapping(target = "codes", expression = "java(java.util.List.of(ac.getCode()))")
    GenerateCodesResponse toGenerateResponse(AccessCode ac);

    @Mapping(target = "targetType", expression = "java(ac.getTargetType().name())")
    @Mapping(target = "targetName", expression = "java(getTargetName(ac))")
    RedeemCodeResponse toRedeemResponse(AccessCode ac, int enrollmentsCount, List<String> unlockedCourses);

    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "studentName", source = "studentName")
    CodeUsageDto toUsageDto(AccessCodeUsage usage);

    List<CodeUsageDto> toUsageDtoList(List<AccessCodeUsage> usages);

    @Named("getTargetName")
    default String getTargetName(AccessCode ac) {
        if (ac.getTargetType() == null) return null;
        return switch (ac.getTargetType()) {
            case CATEGORY -> ac.getCategory() != null ? ac.getCategory().getName() : null;
            case COURSE   -> ac.getCourse() != null ? ac.getCourse().getTitle() : null;
            case SESSION  -> ac.getSession() != null ? ac.getSession().getTitle() : null;
            case WALLET   -> "شحن محفظة";
        };
    }
}
