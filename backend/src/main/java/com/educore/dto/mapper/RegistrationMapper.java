package com.educore.dto.mapper;

import com.educore.dto.request.CompleteRegisterRequest;
import com.educore.dto.response.CompleteRegisterResponse;
import com.educore.dto.response.PhoneCheckResponse;
import com.educore.dto.response.StartRegisterResponse;
import com.educore.student.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {
    @Mapping(target = "action", constant = "VERIFY_OTP")  // أضف هذا
    @Mapping(target = "otpCode", source = "otp") // أضف هذا السطر
    StartRegisterResponse toStartRegisterResponse(String message, String phone, String otp);

    @Mapping(target = "message", source = "message")
    @Mapping(target = "phone", source = "student.phone")
    @Mapping(target = "token", source = "token")
    @Mapping(target = "studentCode", source = "student.studentCode")
    CompleteRegisterResponse toCompleteRegisterResponse(String message, Student student, String token);

    @Mapping(target = "exists", expression = "java(student != null)")
    @Mapping(target = "message", expression = "java(student != null ? \"الرقم مسجل بالفعل\" : \"يمكنك التسجيل بهذا الرقم\")")
    @Mapping(target = "status", source = "student.status")
    @Mapping(target = "studentCode", source = "student.studentCode")
    PhoneCheckResponse toPhoneCheckResponse(Student student);
}