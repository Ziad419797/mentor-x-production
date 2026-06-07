package com.educore.centerschedule;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CenterScheduleRepository extends JpaRepository<CenterSchedule, Long> {
    List<CenterSchedule> findByActiveTrueOrderByDayOfWeekAscStartTimeAsc();
}
