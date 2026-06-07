package com.educore.parent;

import com.educore.common.GlobalResponse;
import com.educore.dto.request.ParentCompleteLoginRequest;
import com.educore.dto.request.ParentStartLoginRequest;
import com.educore.dto.response.ParentCompleteLoginResponse;
import com.educore.dto.response.ParentStartLoginResponse;
import com.educore.security.JwtUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
public class ParentLoginController {

    private final ParentLoginService parentLoginService;

    @PostMapping("/start-login")
    public ParentStartLoginResponse startLogin(@RequestBody @Valid ParentStartLoginRequest request) {
        return parentLoginService.startLogin(request);
    }

    @PostMapping("/complete-login")
    public ParentCompleteLoginResponse completeLogin(@RequestBody @Valid ParentCompleteLoginRequest request) {
        return parentLoginService.completeLogin(request);
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<Void>> logout(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            HttpServletRequest request) {

        String token = extractBearerToken(request);
        parentLoginService.logout(principal.getUserId(), token);
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تسجيل الخروج بنجاح")
                .build());
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
