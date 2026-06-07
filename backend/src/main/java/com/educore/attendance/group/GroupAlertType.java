package com.educore.attendance.group;

public enum GroupAlertType {
    /**
     * الطالب مسجّل في سنتر مختلف عن سنتر الجروب.
     * مثال: الجروب "سنتر المقطم" لكن الطالب مسجّل في "سنتر الدقي".
     */
    WRONG_CENTER,

    /**
     * الطالب مسجّل كـ online (student.online = true)
     * لكنه جاء يحضر في سنتر فعلي.
     */
    ONLINE_TO_CENTER
}
