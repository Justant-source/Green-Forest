package com.vgc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService {

    private static final Logger ACTIVITY = LoggerFactory.getLogger("ACTIVITY");
    private static final Logger SYSTEM = LoggerFactory.getLogger("SYSTEM");

    // ===== 인증 =====

    public void logLogin(String email, String ip) {
        ACTIVITY.info("[LOGIN] email={} ip={}", email, ip);
    }

    public void logLoginFailed(String email, String ip) {
        ACTIVITY.info("[LOGIN_FAILED] email={} ip={}", email, ip);
        SYSTEM.warn("[LOGIN_FAILED] email={} ip={}", email, ip);
    }

    public void logRegister(String email, String nickname) {
        ACTIVITY.info("[REGISTER] email={} nickname={}", email, nickname);
    }

    // ===== 게시글 =====

    public void logPostCreate(Long userId, String nickname, String category, Long postId) {
        ACTIVITY.info("[POST_CREATE] userId={} nickname={} category={} postId={}", userId, nickname, category, postId);
    }

    public void logPostUpdate(Long userId, String nickname, Long postId) {
        ACTIVITY.info("[POST_UPDATE] userId={} nickname={} postId={}", userId, nickname, postId);
    }

    public void logPostDelete(Long userId, String nickname, Long postId) {
        ACTIVITY.info("[POST_DELETE] userId={} nickname={} postId={}", userId, nickname, postId);
    }

    public void logPostLike(Long userId, String nickname, Long postId) {
        ACTIVITY.info("[POST_LIKE] userId={} nickname={} postId={}", userId, nickname, postId);
    }

    // ===== 댓글 =====

    public void logCommentCreate(Long userId, String nickname, Long postId, Long commentId) {
        ACTIVITY.info("[COMMENT_CREATE] userId={} nickname={} postId={} commentId={}", userId, nickname, postId, commentId);
    }

    // ===== 출석 =====

    public void logAttendance(Long userId, String nickname, int dropsAwarded) {
        ACTIVITY.info("[ATTENDANCE] userId={} nickname={} dropsAwarded={}", userId, nickname, dropsAwarded);
    }

    public void logAttendanceDenied(Long userId, String nickname, String reason) {
        ACTIVITY.info("[ATTENDANCE_DENIED] userId={} nickname={} reason={}", userId, nickname, reason);
    }

    // ===== 뽑기 =====

    public void logGachaDraw(Long userId, String nickname, String prizeName, boolean isWinner, Long drawId) {
        ACTIVITY.info("[GACHA_DRAW] userId={} nickname={} prize={} winner={} drawId={}", userId, nickname, prizeName, isWinner, drawId);
    }

    public void logGachaLimitExceeded(Long userId, String nickname) {
        ACTIVITY.info("[GACHA_LIMIT] userId={} nickname={} reason=일일횟수초과", userId, nickname);
    }

    // ===== 물방울 =====

    public void logDropGift(Long senderId, String senderNickname, Long receiverId, String receiverNickname, int amount) {
        ACTIVITY.info("[DROP_GIFT] senderId={} sender={} receiverId={} receiver={} amount={}", senderId, senderNickname, receiverId, receiverNickname, amount);
    }

    public void logDropAward(Long userId, String nickname, int amount, String reason) {
        ACTIVITY.info("[DROP_AWARD] userId={} nickname={} amount={} reason={}", userId, nickname, amount, reason);
    }

    // ===== 시스템 에러 =====

    public void logSystemWarn(String context, String message) {
        SYSTEM.warn("[{}] {}", context, message);
    }

    public void logSystemWarn(String context, String message, Throwable t) {
        SYSTEM.warn("[{}] {}", context, message, t);
    }

    public void logSystemError(String context, String message, Throwable t) {
        SYSTEM.error("[{}] {}", context, message, t);
    }

    public void logSystemError(String context, String message) {
        SYSTEM.error("[{}] {}", context, message);
    }
}
