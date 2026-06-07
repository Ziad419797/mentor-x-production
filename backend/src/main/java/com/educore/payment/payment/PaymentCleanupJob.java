package com.educore.payment.payment;


import com.educore.payment.order.OrderRepository;
import com.educore.payment.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PaymentCleanupJob — ينظف الطلبات المعلقة القديمة
 * ─────────────────────────────────────────────────
 * أي طلب PENDING أكثر من 30 دقيقة بدون دفع → CANCELLED
 * يشتغل كل 15 دقيقة
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupJob {

    private final OrderRepository orderRepo;

    private static final int PENDING_TIMEOUT_MINUTES = 30;

    @Scheduled(fixedDelay = 15 * 60 * 1000) // كل 15 دقيقة
    @Transactional
    public void cancelStalePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES);

        var staleOrders = orderRepo.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);

        if (staleOrders.isEmpty()) return;

        log.info("PaymentCleanupJob: found {} stale pending orders", staleOrders.size());

        staleOrders.forEach(order -> {
            try {
                order.cancel();
                orderRepo.save(order);
            } catch (Exception ex) {
                log.warn("Failed to cancel order {}: {}", order.getOrderNumber(), ex.getMessage());
            }
        });

        log.info("PaymentCleanupJob: cancelled {} orders", staleOrders.size());
    }
}


