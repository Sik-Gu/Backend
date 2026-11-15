package Sikgu.com.example.Web.domain.block.dto.response;

import java.time.LocalDateTime;

/**
 * 차단 API 응답 DTO 모음.
 */
public final class BlockResponses {

    /** POST /api/users/{userId}/block */
    public record BlockResponse(Long blockedUserId, LocalDateTime blockedAt) {
    }

    /** GET /api/users/me/blocks 항목 */
    public record BlockedUser(Long userId, String nickname, String dongName, LocalDateTime blockedAt) {
    }

    private BlockResponses() {
    }
}
