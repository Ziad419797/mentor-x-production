package com.educore.DtoEnroll.mapper;


import com.educore.DtoEnroll.request.EnrollmentRequest;
import com.educore.DtoEnroll.response.EnrollmentResponse;
import com.educore.DtoEnroll.response.EnrollmentStatsResponse;
import com.educore.enrollment.Enrollment;
import com.educore.enrollment.EnrollmentStatus;
import com.educore.student.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, EnrollmentStatus.class})
public interface EnrollmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "enrolledAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "progress", constant = "0.0")
    @Mapping(target = "status", expression = "java(EnrollmentStatus.ACTIVE)")
    @Mapping(target = "lastAccessedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "totalWatchTimeSeconds", constant = "0L")
    @Mapping(target = "completedLessonsCount", constant = "0")
    @Mapping(target = "totalLessonsCount", constant = "0")
    @Mapping(target = "quizzesTaken", constant = "0")
    @Mapping(target = "quizzesPassed", constant = "0")
    @Mapping(target = "averageQuizScore", constant = "0.0")
    @Mapping(target = "assignmentsSubmitted", constant = "0")
    @Mapping(target = "averageAssignmentScore", constant = "0.0")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "accessCount", constant = "0")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "createdBy", constant = "SYSTEM")
    @Mapping(target = "updatedBy", ignore = true)
    Enrollment toEntity(EnrollmentRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student", qualifiedByName = "getStudentFullName")
    @Mapping(target = "studentPhone", source = "student.phone")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "courseDescription", source = "course.description")
    @Mapping(target = "courseImageUrl", source = "course.imageUrl")
    @Mapping(target = "enrolledAt", source = "enrolledAt")
    @Mapping(target = "completedAt", source = "completedAt")
    @Mapping(target = "progress", source = "progress")
    @Mapping(target = "status", source = "status", qualifiedByName = "getStatusDisplay")
    @Mapping(target = "lastAccessedAt", source = "lastAccessedAt")
    @Mapping(target = "totalWatchTimeSeconds", source = "totalWatchTimeSeconds")
    @Mapping(target = "completedLessonsCount", source = "completedLessonsCount")
    @Mapping(target = "totalLessonsCount", source = "totalLessonsCount")
    @Mapping(target = "quizzesTaken", source = "quizzesTaken")
    @Mapping(target = "quizzesPassed", source = "quizzesPassed")
    @Mapping(target = "averageQuizScore", source = "averageQuizScore")
    @Mapping(target = "assignmentsSubmitted", source = "assignmentsSubmitted")
    @Mapping(target = "averageAssignmentScore", source = "averageAssignmentScore")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "remainingDays", expression = "java(getRemainingDays(enrollment.getExpiresAt()))")
    @Mapping(target = "accessCount", source = "accessCount")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "isValidAccess", expression = "java(enrollment.isValidAccess())")
    @Mapping(target = "completionPercentage", expression = "java(String.format(\"%.1f%%\", enrollment.getProgress()))")
    @Mapping(target = "timeSpentFormatted", expression = "java(formatWatchTime(enrollment.getTotalWatchTimeSeconds()))")
    EnrollmentResponse toResponse(Enrollment enrollment);

    List<EnrollmentResponse> toResponseList(List<Enrollment> enrollments);

    @Named("getStudentFullName")
    default String getStudentFullName(Student student) {
        return student != null ? student.getFullName() : null;
    }

    @Named("getStatusDisplay")
    default String getStatusDisplay(EnrollmentStatus status) {
        if (status == null) return null;

        switch (status) {
            case ACTIVE: return "جاري الدراسة";
            case COMPLETED: return "مكتمل";
            case EXPIRED: return "منتهي الصلاحية";
            case CANCELLED: return "ملغي";
            case SUSPENDED: return "معلق";
            default: return status.name();
        }
    }

    default Long getRemainingDays(LocalDateTime expiresAt) {
        if (expiresAt == null) return null;
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
    }

    default String formatWatchTime(Long seconds) {
        if (seconds == null || seconds == 0) return "0 دقيقة";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return hours + " ساعة و " + minutes + " دقيقة";
        } else {
            return minutes + " دقيقة";
        }
    }

    // ==================== Stats Mappers ====================

    default EnrollmentStatsResponse toStatsResponse(
            Long totalEnrollments,
            Long activeEnrollments,
            Long completedEnrollments,
            Long expiredEnrollments,
            Double averageProgress,
            Long totalWatchTime,
            Map<String, Long> enrollmentsByCourse,
            Map<String, Double> progressByCourse,
            Integer totalQuizzes,
            Integer totalAssignments,
            String mostActiveCourse,
            String bestPerformingCourse) {

        return EnrollmentStatsResponse.builder()
                .totalEnrollments(totalEnrollments)
                .activeEnrollments(activeEnrollments)
                .completedEnrollments(completedEnrollments)
                .expiredEnrollments(expiredEnrollments)
                .averageProgress(averageProgress)
                .totalWatchTimeSeconds(totalWatchTime)
                .enrollmentsByCourse(enrollmentsByCourse)
                .progressByCourse(progressByCourse)
                .totalQuizzesTaken(totalQuizzes)
                .totalAssignmentsSubmitted(totalAssignments)
                .mostActiveCourse(mostActiveCourse)
                .bestPerformingCourse(bestPerformingCourse)
                .build();
    }
}