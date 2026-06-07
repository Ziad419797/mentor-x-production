package com.educore.lesson;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * طلب إعادة ترتيب العناصر داخل الحصة (Drag & Drop)
 * يُرسل الفرونتند قائمة بـ id وorder_number لكل عنصر
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {

    @NotNull
    private List<ReorderItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderItem {
        @NotNull
        private Long id;
        @NotNull
        private Integer orderNumber;
    }
}
