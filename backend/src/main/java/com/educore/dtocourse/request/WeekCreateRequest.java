package com.educore.dtocourse.request;

import com.educore.lesson.WeekLockType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeekCreateRequest {

    @NotBlank
    private String title;

    private String description;
    private Integer orderNumber;

    @NotEmpty(message = "At least one session is required")
    private Set<Long> sessionIds;

    // ─── Teacher-configurable lock ─────────────────────────────────
    // اختياري — الافتراضي NEVER (الحصة مفتوحة دايماً)

    /** نوع القفل: NEVER | AFTER_DURATION | ON_DATE */
    @Builder.Default
    private WeekLockType lockType = WeekLockType.NEVER;

    /** عدد أيام الوصول من أول فتح (مع lockType = AFTER_DURATION) */
    @Min(value = 1, message = "lockAfterDays يجب أن يكون على الأقل يوم واحد")
    private Integer lockAfterDays;

    /** التاريخ الثابت للقفل (مع lockType = ON_DATE) */
    private LocalDate lockDate;

    /** هل الوصول للمحتوى بالترتيب فقط؟ (default: false) */
    @Builder.Default
    private boolean requiresSequentialAccess = false;
}

