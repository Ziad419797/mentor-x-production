package com.educore.dtopayment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "يجب إضافة منتج واحد على الأقل")
    @Size(max = 20, message = "لا يمكن إضافة أكثر من 20 منتج في طلب واحد")
    private List<PurchaseItemDto> items;
}