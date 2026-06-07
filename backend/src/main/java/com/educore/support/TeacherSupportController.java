package com.educore.support;

import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/support-channels")
@RequiredArgsConstructor
public class TeacherSupportController {

    private final SupportChannelRepository repo;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<SupportChannelDto>>> getAll(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        List<SupportChannelDto> list = repo
                .findByTeacherIdOrderByDisplayOrderAsc(p.getUserId())
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(GlobalResponse.success("", list));
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<SupportChannelDto>> create(
            @AuthenticationPrincipal JwtUserPrincipal p,
            @RequestBody SupportChannelDto dto) {
        SupportChannel ch = SupportChannel.builder()
                .teacherId(p.getUserId())
                .groupName(dto.getGroupName())
                .grade(dto.getGrade())
                .label(dto.getLabel())
                .type(SupportChannel.ChannelType.valueOf(dto.getType()))
                .value(dto.getValue())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        return ResponseEntity.ok(GlobalResponse.success("تم الإضافة", toDto(repo.save(ch))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<SupportChannelDto>> update(
            @AuthenticationPrincipal JwtUserPrincipal p,
            @PathVariable Long id,
            @RequestBody SupportChannelDto dto) {
        SupportChannel ch = repo.findById(id)
                .filter(c -> c.getTeacherId().equals(p.getUserId()))
                .orElseThrow();
        ch.setGroupName(dto.getGroupName());
        ch.setGrade(dto.getGrade() != null && !dto.getGrade().isBlank() ? dto.getGrade() : null);
        ch.setLabel(dto.getLabel());
        ch.setType(SupportChannel.ChannelType.valueOf(dto.getType()));
        ch.setValue(dto.getValue());
        if (dto.getDisplayOrder() != null) ch.setDisplayOrder(dto.getDisplayOrder());
        return ResponseEntity.ok(GlobalResponse.success("تم التعديل", toDto(repo.save(ch))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponse<Void>> delete(
            @AuthenticationPrincipal JwtUserPrincipal p,
            @PathVariable Long id) {
        repo.findById(id).filter(c -> c.getTeacherId().equals(p.getUserId()))
                .ifPresent(repo::delete);
        return ResponseEntity.ok(GlobalResponse.success("تم الحذف", null));
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
