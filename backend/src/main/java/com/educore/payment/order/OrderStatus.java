package com.educore.payment.order;

public enum OrderStatus {
    PENDING,        // طلب جديد لسه ما اتدفعش
    AWAITING_APPROVAL, // كاش / تحويل بنكي بينتظر موافقة الأدمن
    PAID,           // اتدفع بنجاح
    FAILED,         // فشل الدفع
    CANCELLED,      // اتلغى
    REFUNDED        // رجع فلوس
}
