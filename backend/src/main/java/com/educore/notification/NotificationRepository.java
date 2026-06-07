package com.educore.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** كل إشعارات مستخدم مرتبة من الأحدث */
    Page<Notification> findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(
            Long recipientId, String recipientRole, Pageable pageable);

    /** الإشعارات الغير مقروءة فقط */
    Page<Notification> findByRecipientIdAndRecipientRoleAndIsReadFalseOrderByCreatedAtDesc(
            Long recipientId, String recipientRole, Pageable pageable);

    /** عدد الإشعارات الغير مقروءة — للـ badge */
    long countByRecipientIdAndRecipientRoleAndIsReadFalse(
            Long recipientId, String recipientRole);

    /** عدد الإشعارات الغير مقروءة المرتبطة بطالب محدد (لبادج كل ابن في dashboard ولي الأمر) */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.recipientId = :recipientId
          AND n.recipientRole = :role
          AND n.studentId = :studentId
          AND n.isRead = false
    """)
    long countUnreadByRecipientAndStudent(
            @Param("recipientId") Long recipientId,
            @Param("role") String role,
            @Param("studentId") Long studentId);

    /** تحديد كل إشعارات مستخدم كمقروءة */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP
        WHERE n.recipientId = :recipientId
          AND n.recipientRole = :role
          AND n.isRead = false
    """)
    int markAllAsRead(@Param("recipientId") Long recipientId,
                      @Param("role") String role);
}
