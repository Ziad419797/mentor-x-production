package com.educore.auth;

/**
 * @deprecated استخدم {@link com.educore.common.GlobalResponse} بدلاً منه.
 * هذا الـ class موجود فقط للتوافق مع الـ controllers الموجودة.
 * تم توحيد الـ implementation في com.educore.common.GlobalResponse.
 *
 * <p>مثال على الاستخدام الصحيح:
 * <pre>
 *   import com.educore.common.GlobalResponse;
 *   return ResponseEntity.ok(GlobalResponse.success("تمت العملية", data));
 * </pre>
 */
@Deprecated(forRemoval = true)
public class GlobalResponse<T> extends com.educore.common.GlobalResponse<T> {

    public GlobalResponse() {
        super();
    }

    /**
     * Factory method بالترتيب القديم (data أولاً) للتوافق مع الكود الموجود.
     * في الكود الجديد استخدم: GlobalResponse.success(message, data)
     */
    public static <T> com.educore.common.GlobalResponse<T> success(T data, String message) {
        return com.educore.common.GlobalResponse.<T>success(message, data);
    }

    public static <T> com.educore.common.GlobalResponse<T> success(T data) {
        return com.educore.common.GlobalResponse.<T>success(data);
    }
}