package com.educore.dtocourse.mapper;

import com.educore.category.Category;
import com.educore.course.Course;
import com.educore.dtocourse.request.CreateCourseRequest;
import com.educore.dtocourse.request.UpdateCourseRequest;
import com.educore.dtocourse.response.CourseResponse;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "price", source = "price")  // ✅ أضف هذا
    Course toEntity(CreateCourseRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "imageUrl", source = "imageUrl")  // ← دي هتضيف الصورة
    @Mapping(target = "price", source = "price")  // ✅ أضف هذا

    void updateEntityFromRequest(UpdateCourseRequest request, @MappingTarget Course course);

    @Mapping(target = "categoryIds", expression = "java(mapCategoriesToIds(course.getCategories()))")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "discountedPrice", source = "discountedPrice")
    @Mapping(target = "discountPercentage", source = "discountPercentage")
    @Mapping(target = "accessDays", source = "accessDays")
    @Mapping(target = "accessExpiresAt", source = "accessExpiresAt")
    @Mapping(target = "courseType", source = "courseType")
    @Mapping(target = "teachingType", source = "teachingType")
    @Mapping(target = "studentPoints", source = "studentPoints")
    @Mapping(target = "contentOrder", source = "contentOrder")
    @Mapping(target = "trackAttendance", source = "trackAttendance")
    @Mapping(target = "featured", source = "featured")
    @Mapping(target = "pinned", source = "pinned")
    @Mapping(target = "enrolledStudentsCount", ignore = true)
    CourseResponse toResponse(Course course);

    default Set<Long> mapCategoriesToIds(Set<Category> categories) {
        if (categories == null) return null;
        return categories.stream().map(Category::getId).collect(Collectors.toSet());
    }
}
