package com.educore.support;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportChannelRepository extends JpaRepository<SupportChannel, Long> {
    List<SupportChannel> findByTeacherIdOrderByDisplayOrderAsc(Long teacherId);
    List<SupportChannel> findByTeacherIdAndGradeIsNullOrTeacherIdAndGradeOrderByDisplayOrderAsc(Long t1, Long t2, String grade);
}
