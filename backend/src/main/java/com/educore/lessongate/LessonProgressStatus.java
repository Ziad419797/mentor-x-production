package com.educore.lessongate;

public enum LessonProgressStatus {
    LOCKED,       // مش متاحة للطالب بعد
    UNLOCKED,     // متاحة — لسه ما بدأش
    IN_PROGRESS,  // الطالب فاتحها دلوقتي
    COMPLETED     // خلّصها وعدى الكويز/الواجب
}
