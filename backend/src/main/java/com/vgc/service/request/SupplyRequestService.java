package com.vgc.service.request;

import com.vgc.entity.NotificationType;
import com.vgc.entity.RequestKind;
import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.RequestStatus;
import com.vgc.entity.SupplyRequest;
import com.vgc.entity.User;
import com.vgc.repository.SupplyRequestRepository;
import com.vgc.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SupplyRequestService {

    private static final int TITLE_MAX = 80;
    private static final int QUANTITY_MIN = 1;
    private static final int QUANTITY_MAX = 5;
    private static final int MEMO_MAX = 300;
    private static final int REJECT_NOTE_MAX = 100;

    private final SupplyRequestRepository supplyRequestRepository;
    private final NotificationService notificationService;

    public SupplyRequestService(SupplyRequestRepository supplyRequestRepository,
                                NotificationService notificationService) {
        this.supplyRequestRepository = supplyRequestRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SupplyRequest create(User user, String title, int quantity, String memo) {
        String normalizedTitle = requireTitle(title);
        requireQuantity(quantity);
        String normalizedMemo = requireMemo(memo);

        SupplyRequest request = new SupplyRequest();
        request.setUser(user);
        request.setTitle(normalizedTitle);
        request.setQuantity(quantity);
        request.setMemo(normalizedMemo);
        request.setStatus(RequestStatus.PENDING);
        return supplyRequestRepository.save(request);
    }

    @Transactional
    public SupplyRequest update(User user, Long id, String title, Integer quantity, String memo) {
        SupplyRequest request = requireOwnPending(user, id);
        if (title != null) {
            request.setTitle(requireTitle(title));
        }
        if (quantity != null) {
            requireQuantity(quantity);
            request.setQuantity(quantity);
        }
        if (memo != null) {
            request.setMemo(requireMemo(memo));
        }
        return supplyRequestRepository.save(request);
    }

    @Transactional
    public void cancel(User user, Long id) {
        SupplyRequest request = requireOwnPending(user, id);
        supplyRequestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<SupplyRequest> listMine(User user) {
        List<SupplyRequest> pending = supplyRequestRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), RequestStatus.PENDING);
        List<SupplyRequest> others = supplyRequestRepository
                .findTop20ByUserIdAndStatusNotOrderByCreatedAtDesc(user.getId(), RequestStatus.PENDING);
        List<SupplyRequest> result = new ArrayList<>(pending.size() + others.size());
        result.addAll(pending);
        result.addAll(others);
        return result;
    }

    @Transactional(readOnly = true)
    public List<SupplyRequest> listForAdmin(RequestStatus status) {
        RequestStatus filter = status == null ? RequestStatus.PENDING : status;
        return supplyRequestRepository.findByStatusOrderByCreatedAtDesc(filter);
    }

    @Transactional(readOnly = true)
    public List<SupplyRequest> listHistory() {
        List<SupplyRequest> rows = supplyRequestRepository.findProcessedWithUser();
        if (rows.size() > 200) {
            return new ArrayList<>(rows.subList(0, 200));
        }
        return rows;
    }

    @Transactional
    public void purchase(Long id) {
        SupplyRequest request = requirePendingForAdmin(id);
        request.setStatus(RequestStatus.PURCHASED);
        request.setPurchasedAt(LocalDateTime.now());
        supplyRequestRepository.save(request);
        notificationService.createNotification(
                request.getUser(),
                NotificationType.REQUEST,
                "사무용품 구매 완료",
                request.getTitle() + " " + request.getQuantity() + "개",
                null,
                null,
                RequestKind.SUPPLY);
    }

    @Transactional
    public void reject(Long id, RequestRejectReason reason, String note) {
        SupplyRequest request = requirePendingForAdmin(id);
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거절 사유를 선택하세요.");
        }
        String storedNote = null;
        if (reason == RequestRejectReason.OTHER) {
            storedNote = requireOtherNote(note);
        }
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectReason(reason);
        request.setRejectNote(storedNote);
        request.setRejectedAt(LocalDateTime.now());
        supplyRequestRepository.save(request);

        String body = request.getTitle() + " · " + reason.getLabel();
        if (reason == RequestRejectReason.OTHER) {
            body = body + " · " + storedNote;
        }
        notificationService.createNotification(
                request.getUser(),
                NotificationType.REQUEST,
                "사무용품 요청 거절",
                body,
                null,
                null,
                RequestKind.SUPPLY);
    }

    private SupplyRequest requireOwnPending(User user, Long id) {
        SupplyRequest request = findOr404(id);
        if (request.getUser() == null || !user.getId().equals(request.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 요청만 처리할 수 있습니다.");
        }
        requirePending(request);
        return request;
    }

    private SupplyRequest requirePendingForAdmin(Long id) {
        SupplyRequest request = findOr404(id);
        requirePending(request);
        return request;
    }

    private SupplyRequest findOr404(Long id) {
        return supplyRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));
    }

    private void requirePending(SupplyRequest request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "대기 중인 요청만 처리할 수 있습니다.");
        }
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름을 입력하세요.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > TITLE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름은 80자 이하여야 합니다.");
        }
        return trimmed;
    }

    private void requireQuantity(int quantity) {
        if (quantity < QUANTITY_MIN || quantity > QUANTITY_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1~5개여야 합니다.");
        }
    }

    private String requireMemo(String memo) {
        if (memo == null) {
            return null;
        }
        if (memo.length() > MEMO_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메모는 300자 이하여야 합니다.");
        }
        return memo.isBlank() ? null : memo;
    }

    private String requireOtherNote(String note) {
        if (note == null || note.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기타는 사유를 입력하세요.");
        }
        String trimmed = note.trim();
        if (trimmed.length() > REJECT_NOTE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기타 사유는 100자 이하여야 합니다.");
        }
        return trimmed;
    }
}
