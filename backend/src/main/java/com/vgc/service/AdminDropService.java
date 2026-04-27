package com.vgc.service;

import com.vgc.dto.AdminDropTransactionResponse;
import com.vgc.entity.DropReasonType;
import com.vgc.entity.DropTransaction;
import com.vgc.entity.NotificationType;
import com.vgc.entity.User;
import com.vgc.repository.DropTransactionRepository;
import com.vgc.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 관리자 전용 물방울 이력 조회/회수/수정.
 *
 * - 회수: 원본 row 를 삭제하고 사용자 잔고를 해당 금액만큼 역산한다. (감사 로그는 ActivityLog)
 * - 수정: 원본 row 의 amount 를 갱신하고 차액을 사용자 잔고에 반영한다.
 */
@Service
public class AdminDropService {

    private final DropTransactionRepository dropTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public AdminDropService(DropTransactionRepository dropTransactionRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            ActivityLogService activityLogService) {
        this.dropTransactionRepository = dropTransactionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public Page<AdminDropTransactionResponse> list(List<DropReasonType> types, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<DropTransaction> txs = (types == null || types.isEmpty())
                ? dropTransactionRepository.findAllForAdmin(pageable)
                : dropTransactionRepository.findByReasonTypesForAdmin(types, pageable);
        return txs.map(AdminDropTransactionResponse::from);
    }

    @Transactional
    public void revoke(Long transactionId, User admin) {
        DropTransaction tx = dropTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다."));

        User target = tx.getUser();
        int amount = tx.getAmount();

        // 잔고 역산
        target.setTotalDrops(target.getTotalDrops() - amount);
        if (amount > 0) {
            // earned_drops 는 누적 획득분 (양수만). 회수 시 원래 가산된 만큼 차감.
            target.setEarnedDrops(Math.max(0, target.getEarnedDrops() - amount));
        }
        userRepository.save(target);

        dropTransactionRepository.delete(tx);

        activityLogService.logSystemError("DROP_REVOKE",
                "admin=" + admin.getNickname() + " target=" + target.getNickname()
                        + " amount=" + amount + " reason=" + tx.getReasonType()
                        + " detail=" + tx.getReasonDetail(), null);

        notificationService.createNotification(target, NotificationType.DROP_AWARD,
                "물방울 회수",
                "관리자 조치로 💧" + Math.abs(amount) + " 물방울이 " + (amount > 0 ? "회수" : "복원") + "되었습니다.",
                tx.getRelatedPostId(), tx.getRelatedQuestId());
    }

    @Transactional
    public AdminDropTransactionResponse adjust(Long transactionId, int newAmount, User admin) {
        DropTransaction tx = dropTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다."));

        User target = tx.getUser();
        int previous = tx.getAmount();
        int delta = newAmount - previous;

        if (delta != 0) {
            target.setTotalDrops(target.getTotalDrops() + delta);
            // earned_drops 는 양수 누적분 — 이전 양수 기록분을 차감하고 새 양수분을 가산
            int previousPositive = Math.max(0, previous);
            int newPositive = Math.max(0, newAmount);
            int earnedDelta = newPositive - previousPositive;
            target.setEarnedDrops(Math.max(0, target.getEarnedDrops() + earnedDelta));
            userRepository.save(target);
        }

        tx.setAmount(newAmount);
        dropTransactionRepository.save(tx);

        activityLogService.logSystemError("DROP_ADJUST",
                "admin=" + admin.getNickname() + " txId=" + transactionId
                        + " " + previous + " → " + newAmount, null);

        if (delta != 0) {
            notificationService.createNotification(target, NotificationType.DROP_AWARD,
                    "물방울 금액 조정",
                    "관리자 조치로 거래 금액이 " + previous + " → " + newAmount + " 로 변경되었습니다.",
                    tx.getRelatedPostId(), tx.getRelatedQuestId());
        }

        return AdminDropTransactionResponse.from(tx);
    }
}
