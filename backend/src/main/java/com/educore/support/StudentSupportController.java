package com.educore.support;

import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/support-channels")
@RequiredArgsConstructor
public class StudentSupportController {

    private final SupportChannelRepository repo;
    private final StudentRepository studentRepository;

    /** Returns channels for the teacher, filtered to match student's grade (or null=all) */
    @GetMapping
    public ResponseEntity<GlobalResponse<List<SupportChannelDto>>> get(
            @AuthenticationPrincipal JwtUserPrincipal p) {

        Student student = studentRepository.findById(p.getUserId()).orElseThrow();
        String grade = student.getGrade();

        // Find the teacher for this student (assumes single teacher platform)
        // Get all channels, filter: grade==null OR grade==studentGrade
        // We need teacherId — fetch from Teacher table (single teacher assumption)
        List<SupportChannel> all = repo.findAll();
        List<SupportChannelDto> filtered = all.stream()
                .filter(c -> c.getGrade() == null || c.getGrade().isBlank()
                        || (grade != null && grade.equals(c.getGrade())))
                .sorted(Comparator.comparingInt(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 0))
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(GlobalResponse.success("", filtered));
    }

    private SupportChannelDto toDto(SupportChannel c) {
        SupportChannelDto d = new SupportChannelDto();
        d.setId(c.getId());
        d.setGroupName(c.getGroupName());
        d.setGrade(c.getGrade());
        d.setLabel(c.getLabel());
        d.setType(c.getType().name());
        d.setValue(c.getValue());
        d.setDisplayOrder(c.getDisplayOrder());
        return d;
    }
}
