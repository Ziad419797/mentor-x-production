package com.educore.dtopayment.mapper;

import com.educore.dtopayment.*;
import com.educore.payment.order.Order;
import com.educore.payment.order.OrderItem;
import com.educore.payment.payment.Payment;
import com.educore.wallet.Wallet;
import com.educore.wallet.WalletTransaction;
import org.mapstruct.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {LocalDateTime.class, DateTimeFormatter.class}
)
public interface PaymentMapper {

    /* ══════════════════════════════════════════
       Wallet
    ══════════════════════════════════════════ */

    @Mapping(target = "studentId",       source = "student.id")
    @Mapping(target = "studentName",     source = "student",
            qualifiedByName = "studentFullName")
    @Mapping(target = "formattedBalance",source = "balance",
            qualifiedByName = "formatMoney")
    WalletDto toWalletDto(Wallet wallet);

    /* ══════════════════════════════════════════
       WalletTransaction
    ══════════════════════════════════════════ */

    @Mapping(target = "walletId",      source = "wallet.id")
    @Mapping(target = "studentId",     source = "wallet.student.id")
    @Mapping(target = "studentName",   source = "wallet.student.fullName")
    @Mapping(target = "studentCode",   source = "wallet.student.studentCode")
    @Mapping(target = "type",          expression = "java(tx.getType().name())")
    @Mapping(target = "status",        expression = "java(tx.getStatus().name())")
    @Mapping(target = "paymentMethod", expression = "java(tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null)")
    @Mapping(target = "formattedAmount",source = "amount", qualifiedByName = "formatMoney")
    @Mapping(target = "formattedDate",  source = "createdAt", qualifiedByName = "formatDate")
    WalletTransactionDto toWalletTransactionDto(WalletTransaction tx);

    List<WalletTransactionDto> toWalletTransactionDtoList(List<WalletTransaction> list);

    /* ══════════════════════════════════════════
       Order
    ══════════════════════════════════════════ */

    @Mapping(target = "studentId",    source = "student.id")
    @Mapping(target = "studentName",  source = "student",
            qualifiedByName = "studentFullName")
    @Mapping(target = "status",       expression = "java(order.getStatus().name())")
    @Mapping(target = "paymentMethod",expression = "java(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)")
    @Mapping(target = "formattedTotal",source = "total", qualifiedByName = "formatMoney")
    @Mapping(target = "items",        source = "items")
    @Mapping(target = "payment",      source = "payment")
    OrderDto toOrderDto(Order order);

    List<OrderDto> toOrderDtoList(List<Order> orders);

    /* ══════════════════════════════════════════
       OrderItem
    ══════════════════════════════════════════ */

    @Mapping(target = "productId", expression = "java(resolveProductId(item))")
    OrderItemDto toOrderItemDto(OrderItem item);

    List<OrderItemDto> toOrderItemDtoList(List<OrderItem> items);

    /* ══════════════════════════════════════════
       Payment
    ══════════════════════════════════════════ */

    @Mapping(target = "orderId",      source = "order.id")
    @Mapping(target = "orderNumber",  source = "order.orderNumber")
    @Mapping(target = "paymentMethod",expression = "java(payment.getPaymentMethod().name())")
    @Mapping(target = "status",       expression = "java(payment.getStatus().name())")
    PaymentDto toPaymentDto(Payment payment);

    /* ══════════════════════════════════════════
       Named Helpers
    ══════════════════════════════════════════ */

    @Named("studentFullName")
    default String studentFullName(com.educore.student.Student student) {
        if (student == null) return null;
        return student.getFullName();
    }

    @Named("formatMoney")
    default String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00 ج.م";
        return String.format("%.2f ج.م", amount);
    }

    @Named("formatDate")
    default String formatDate(LocalDateTime date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    default Long resolveProductId(OrderItem item) {
        if ("CATEGORY".equals(item.getProductType()) && item.getCategory() != null) {
            return item.getCategory().getId();
        }
        if ("COURSE".equals(item.getProductType()) && item.getCourse() != null) {
            return item.getCourse().getId();
        }
        return null;
    }
}