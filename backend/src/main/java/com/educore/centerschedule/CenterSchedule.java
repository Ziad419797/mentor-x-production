package com.educore.centerschedule;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "center_schedules")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CenterSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String centerName;

    @Column(nullable = false)
    private String groupName;

    private String gradeLevel;

    @Column(nullable = false)
    private String dayOfWeek;

    private String startTime;
    private String endTime;
    private String notes;
    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
}
