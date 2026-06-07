package com.educore.student.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/** الطالب يحدّث موقعه الجغرافي */
@Data
public class UpdateLocationRequest {

    /** خط الطول — من الـ GPS في المتصفح/التطبيق */
    @DecimalMin(value = "-90.0",  message = "latitude غير صحيح")
    @DecimalMax(value = "90.0",   message = "latitude غير صحيح")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude غير صحيح")
    @DecimalMax(value = "180.0",  message = "longitude غير صحيح")
    private Double longitude;

    /** عنوان نصي — يكتبه الطالب أو بيجي من Reverse Geocoding في الـ Frontend */
    private String mapAddress;
}
