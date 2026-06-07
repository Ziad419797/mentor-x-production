package com.educore.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtData Record Tests")
class JwtDataTest {

    @Test
    @DisplayName("isExpired يرجع true لو الـ expiry في الماضي")
    void isExpired_pastExpiry_returnsTrue() {
        JwtData data = JwtData.builder()
                .phone("01012345678")
                .role("STUDENT")
                .userId(1L)
                .expiry(LocalDateTime.now().minusMinutes(5))
                .tokenType("ACCESS")
                .build();

        assertTrue(data.isExpired());
        assertFalse(data.isValid());
    }

    @Test
    @DisplayName("isExpired يرجع false لو الـ expiry في المستقبل")
    void isExpired_futureExpiry_returnsFalse() {
        JwtData data = JwtData.builder()
                .phone("01012345678")
                .role("STUDENT")
                .userId(1L)
                .expiry(LocalDateTime.now().plusMinutes(30))
                .tokenType("ACCESS")
                .build();

        assertFalse(data.isExpired());
        assertTrue(data.isValid());
    }

    @Test
    @DisplayName("isStudent يرجع true للـ role = STUDENT")
    void isStudent_studentRole_returnsTrue() {
        JwtData data = JwtData.builder().role("STUDENT").build();
        assertTrue(data.isStudent());
        assertFalse(data.isTeacher());
        assertFalse(data.isParent());
    }

    @Test
    @DisplayName("isTeacher يرجع true للـ role = TEACHER")
    void isTeacher_teacherRole_returnsTrue() {
        JwtData data = JwtData.builder().role("TEACHER").build();
        assertTrue(data.isTeacher());
        assertFalse(data.isStudent());
    }

    @Test
    @DisplayName("getUserDisplayName يرجع الاسم لو موجود")
    void getUserDisplayName_withName_returnsName() {
        JwtData data = JwtData.builder()
                .name("أحمد محمد")
                .phone("01012345678")
                .build();

        assertEquals("أحمد محمد", data.getUserDisplayName());
    }

    @Test
    @DisplayName("getUserDisplayName يرجع رقم الهاتف لو الاسم فاضي")
    void getUserDisplayName_noName_returnsPhone() {
        JwtData data = JwtData.builder()
                .phone("01012345678")
                .build();

        assertEquals("01012345678", data.getUserDisplayName());
    }

    @Test
    @DisplayName("isAccountActive يرجع true للـ status = ACTIVE")
    void isAccountActive_returnsTrue() {
        JwtData data = JwtData.builder().status("ACTIVE").build();
        assertTrue(data.isAccountActive());
        assertFalse(data.isAccountPending());
        assertFalse(data.isAccountRejected());
    }

    @Test
    @DisplayName("getRemainingSeconds يرجع قيمة موجبة للـ token الصالح")
    void getRemainingSeconds_futureExpiry_returnsPositive() {
        JwtData data = JwtData.builder()
                .expiry(LocalDateTime.now().plusMinutes(10))
                .build();

        assertTrue(data.getRemainingSeconds() > 0);
    }

    @Test
    @DisplayName("getRemainingSeconds يرجع 0 لو expiry = null")
    void getRemainingSeconds_nullExpiry_returnsZero() {
        JwtData data = JwtData.builder().build();
        assertEquals(0, data.getRemainingSeconds());
    }
}
