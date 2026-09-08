package com.vgc.service.request;

import com.vgc.dto.request.AdminSnackCatalogItemResponse;
import com.vgc.dto.request.AdminSnackRequestResponse;
import com.vgc.dto.request.SnackCatalogCardResponse;
import com.vgc.dto.request.SnackGroupedPendingResponse;
import com.vgc.dto.request.SnackRequestResponse;
import com.vgc.dto.request.SnackSsgCategoryResponse;
import com.vgc.entity.NotificationType;
import com.vgc.entity.RequestKind;
import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.RequestStatus;
import com.vgc.entity.SnackCatalogItem;
import com.vgc.entity.SnackRequest;
import com.vgc.entity.SnackSsgCategory;
import com.vgc.entity.User;
import com.vgc.repository.SnackCatalogItemRepository;
import com.vgc.repository.SnackRequestRepository;
import com.vgc.service.NotificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SnackRequestService {

    private static final String DUPLICATE_PENDING_MESSAGE = "이미 요청한 상품입니다. 수량을 변경하세요.";
    private static final String OTHER_NOTE_REQUIRED = "기타는 사유를 입력하세요.";
    private static final String EMPTY_GROUP_MESSAGE = "대기 중인 요청이 없습니다.";
    private static final String PURCHASE_TITLE = "간식 구매 완료";
    private static final String REJECT_TITLE = "간식 요청 거절";

    private final SnackCatalogItemRepository catalogItemRepository;
    private final SnackRequestRepository snackRequestRepository;
    private final NotificationService notificationService;

    public SnackRequestService(SnackCatalogItemRepository catalogItemRepository,
                               SnackRequestRepository snackRequestRepository,
                               NotificationService notificationService) {
        this.catalogItemRepository = catalogItemRepository;
        this.snackRequestRepository = snackRequestRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SnackSsgCategoryResponse> listCategories() {
        return Arrays.stream(SnackSsgCategory.values())
                .map(c -> new SnackSsgCategoryResponse(c.name(), c.getLabel()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SnackSsgCategoryResponse> listVisibleCategories() {
        List<SnackSsgCategory> found = catalogItemRepository.findVisibleCategories();
        Set<SnackSsgCategory> visible = new HashSet<>(found == null ? List.of() : found);
        return Arrays.stream(SnackSsgCategory.values())
                .filter(visible::contains)
                .map(c -> new SnackSsgCategoryResponse(c.name(), c.getLabel()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SnackCatalogCardResponse> listCatalog(User user, String q, boolean requestedOnly,
                                                      SnackSsgCategory category, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        Map<Long, Integer> pendingByItem = pendingQtyByItem();
        Map<Long, SnackRequest> mineByItem = myPendingByItem(user.getId());

        if (!requestedOnly) {
            Page<SnackCatalogItem> page = catalogItemRepository.findVisibleCatalog(
                    query, category, defaultDisplayOrder(pageable));
            return page.map(item -> toCard(item, pendingByItem, mineByItem));
        }

        List<SnackCatalogItem> filtered = new ArrayList<>(
                catalogItemRepository.findRequestedCatalog(query, category, Pageable.unpaged()).getContent());
        filtered.sort(Comparator
                .comparingInt((SnackCatalogItem item) -> pendingByItem.getOrDefault(item.getId(), 0))
                .reversed()
                .thenComparingInt(SnackCatalogItem::getDisplayOrder));

        Pageable resultPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("pendingQty"), Sort.Order.asc("displayOrder")));
        int start = (int) resultPageable.getOffset();
        List<SnackCatalogItem> slice;
        if (start >= filtered.size()) {
            slice = List.of();
        } else {
            int end = Math.min(start + resultPageable.getPageSize(), filtered.size());
            slice = filtered.subList(start, end);
        }
        return new PageImpl<>(slice, resultPageable, filtered.size())
                .map(item -> toCard(item, pendingByItem, mineByItem));
    }

    @Transactional
    public SnackRequest create(User user, Long catalogItemId, int quantity) {
        SnackCatalogItem item = catalogItemRepository.findById(catalogItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        validateQuantity(quantity);
        if (!item.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 신청할 수 없는 상품입니다.");
        }
        Optional<SnackRequest> existing = snackRequestRepository
                .findByUserIdAndCatalogItemIdAndStatus(user.getId(), catalogItemId, RequestStatus.PENDING);
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_PENDING_MESSAGE);
        }
        if (item.isHiddenFromUsers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "숨김 처리된 상품입니다.");
        }

        SnackRequest request = new SnackRequest();
        request.setUser(user);
        request.setCatalogItem(item);
        request.setQuantity(1);
        request.setStatus(RequestStatus.PENDING);
        try {
            return snackRequestRepository.save(request);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_PENDING_MESSAGE);
        }
    }

    @Transactional
    public SnackRequest updateQuantity(User user, Long requestId, int quantity) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "간식 수량은 변경할 수 없습니다.");
    }

    @Transactional
    public void cancel(User user, Long requestId) {
        SnackRequest request = requireOwnPending(user, requestId);
        snackRequestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<SnackRequestResponse> listMine(User user) {
        List<SnackRequest> pending = snackRequestRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), RequestStatus.PENDING);
        List<SnackRequest> rest = snackRequestRepository
                .findTop20ByUserIdAndStatusNotOrderByCreatedAtDesc(user.getId(), RequestStatus.PENDING);

        List<SnackRequestResponse> out = new ArrayList<>();
        for (SnackRequest r : pending) {
            out.add(toResponse(r));
        }
        for (SnackRequest r : rest) {
            out.add(toResponse(r));
        }
        return out;
    }

    @Transactional
    public int purchaseAllPendingForItem(Long catalogItemId) {
        List<SnackRequest> pending = snackRequestRepository
                .findByCatalogItemIdAndStatus(catalogItemId, RequestStatus.PENDING);
        if (pending.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, EMPTY_GROUP_MESSAGE);
        }
        LocalDateTime now = LocalDateTime.now();
        for (SnackRequest request : pending) {
            markPurchased(request, now);
            snackRequestRepository.save(request);
            notifyPurchased(request);
        }
        return pending.size();
    }

    @Transactional
    public void purchaseOne(Long requestId) {
        SnackRequest request = requirePendingForAdmin(requestId);
        markPurchased(request, LocalDateTime.now());
        snackRequestRepository.save(request);
        notifyPurchased(request);
    }

    @Transactional
    public void rejectOne(Long requestId, RequestRejectReason reason, String note) {
        SnackRequest request = requirePendingForAdmin(requestId);
        String storedNote = resolveRejectNote(reason, note);
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectReason(reason);
        request.setRejectNote(storedNote);
        request.setRejectedAt(LocalDateTime.now());
        request.setPendingOwnerId(null);
        snackRequestRepository.save(request);
        notifyRejected(request);
    }

    @Transactional
    public int rejectAllPendingForItem(Long catalogItemId, RequestRejectReason reason, String note) {
        String storedNote = resolveRejectNote(reason, note);
        List<SnackRequest> pending = snackRequestRepository
                .findByCatalogItemIdAndStatus(catalogItemId, RequestStatus.PENDING);
        if (pending.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, EMPTY_GROUP_MESSAGE);
        }
        LocalDateTime now = LocalDateTime.now();
        for (SnackRequest request : pending) {
            request.setStatus(RequestStatus.REJECTED);
            request.setRejectReason(reason);
            request.setRejectNote(storedNote);
            request.setRejectedAt(now);
            request.setPendingOwnerId(null);
            snackRequestRepository.save(request);
            notifyRejected(request);
        }
        return pending.size();
    }

    @Transactional(readOnly = true)
    public List<SnackGroupedPendingResponse> listGroupedPending() {
        List<SnackRequest> pending = snackRequestRepository.findPendingWithItemAndUser();
        Map<Long, SnackGroupedPendingResponse> byItem = new LinkedHashMap<>();
        for (SnackRequest request : pending) {
            SnackCatalogItem item = request.getCatalogItem();
            if (item == null) {
                continue;
            }
            SnackGroupedPendingResponse group = byItem.get(item.getId());
            if (group == null) {
                group = new SnackGroupedPendingResponse();
                group.setCatalogItemId(item.getId());
                group.setName(item.getName());
                group.setImageUrl(item.getImageUrl());
                group.setSsgUrl(SsgSnackCatalogParser.itemViewUrl(item.getSsgItemId()));
                group.setAvailable(item.isAvailable());
                group.setHiddenFromUsers(item.isHiddenFromUsers());
                byItem.put(item.getId(), group);
            }
            SnackGroupedPendingResponse.Line line = new SnackGroupedPendingResponse.Line();
            line.setId(request.getId());
            line.setUserId(request.getUser() != null ? request.getUser().getId() : null);
            line.setNickname(request.getUser() != null ? request.getUser().getNickname() : null);
            line.setQuantity(request.getQuantity());
            line.setCreatedAt(iso(request.getCreatedAt()));
            group.getLines().add(line);
            group.setTotalQty(group.getTotalQty() + request.getQuantity());
            group.setRequesterCount(group.getLines().size());
        }
        List<SnackGroupedPendingResponse> out = new ArrayList<>(byItem.values());
        out.sort(Comparator
                .comparingInt(SnackGroupedPendingResponse::getTotalQty).reversed()
                .thenComparing(SnackGroupedPendingResponse::getName, Comparator.nullsLast(String::compareTo)));
        return out;
    }

    @Transactional(readOnly = true)
    public List<AdminSnackRequestResponse> listHistory() {
        List<SnackRequest> rows = snackRequestRepository.findProcessedWithItemAndUser();
        if (rows.size() > 200) {
            rows = rows.subList(0, 200);
        }
        List<AdminSnackRequestResponse> out = new ArrayList<>();
        for (SnackRequest request : rows) {
            out.add(toAdminHistory(request));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Page<AdminSnackCatalogItemResponse> listAdminCatalog(String q, boolean includeUnavailable,
                                                               SnackSsgCategory category, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        Pageable effective = defaultDisplayOrder(pageable);
        Map<Long, Integer> pendingByItem = pendingQtyByItem();
        return catalogItemRepository.findAdminCatalog(query, includeUnavailable, category, effective)
                .map(item -> toAdminItem(item, pendingByItem));
    }

    public SnackRequestResponse toResponse(SnackRequest request) {
        SnackCatalogItem item = request.getCatalogItem();
        RequestRejectReason reason = request.getRejectReason();
        SnackRequestResponse response = new SnackRequestResponse();
        response.setId(request.getId());
        response.setCatalogItemId(item != null ? item.getId() : null);
        response.setName(item != null ? item.getName() : null);
        response.setImageUrl(item != null ? item.getImageUrl() : null);
        response.setSsgUrl(item != null ? SsgSnackCatalogParser.itemViewUrl(item.getSsgItemId()) : null);
        response.setQuantity(request.getQuantity());
        response.setStatus(request.getStatus());
        response.setRejectReason(reason);
        response.setRejectReasonLabel(reason != null ? reason.getLabel() : null);
        response.setRejectNote(request.getRejectNote());
        response.setCreatedAt(iso(request.getCreatedAt()));
        response.setPurchasedAt(iso(request.getPurchasedAt()));
        response.setRejectedAt(iso(request.getRejectedAt()));
        return response;
    }

    private AdminSnackRequestResponse toAdminHistory(SnackRequest request) {
        AdminSnackRequestResponse response = new AdminSnackRequestResponse();
        SnackRequestResponse base = toResponse(request);
        response.setId(base.getId());
        response.setCatalogItemId(base.getCatalogItemId());
        response.setName(base.getName());
        response.setImageUrl(base.getImageUrl());
        response.setSsgUrl(base.getSsgUrl());
        response.setQuantity(base.getQuantity());
        response.setStatus(base.getStatus());
        response.setRejectReason(base.getRejectReason());
        response.setRejectReasonLabel(base.getRejectReasonLabel());
        response.setRejectNote(base.getRejectNote());
        response.setCreatedAt(base.getCreatedAt());
        response.setPurchasedAt(base.getPurchasedAt());
        response.setRejectedAt(base.getRejectedAt());
        User user = request.getUser();
        response.setUserId(user != null ? user.getId() : null);
        response.setNickname(user != null ? user.getNickname() : null);
        return response;
    }

    private SnackRequest requireOwnPending(User user, Long requestId) {
        SnackRequest request = snackRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));
        if (request.getUser() == null || !user.getId().equals(request.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 요청만 변경할 수 있습니다.");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "대기 중인 요청만 처리할 수 있습니다.");
        }
        return request;
    }

    private SnackRequest requirePendingForAdmin(Long requestId) {
        SnackRequest request = snackRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 요청입니다.");
        }
        return request;
    }

    private void validateQuantity(int quantity) {
        if (quantity != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "간식은 1인당 1개만 요청할 수 있습니다.");
        }
    }

    private String resolveRejectNote(RequestRejectReason reason, String note) {
        if (reason == RequestRejectReason.OTHER) {
            String trimmed = note == null ? "" : note.trim();
            if (trimmed.isEmpty() || trimmed.length() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, OTHER_NOTE_REQUIRED);
            }
            return trimmed;
        }
        return null;
    }

    private void markPurchased(SnackRequest request, LocalDateTime now) {
        request.setStatus(RequestStatus.PURCHASED);
        request.setPurchasedAt(now);
        request.setPendingOwnerId(null);
    }

    private void notifyPurchased(SnackRequest request) {
        SnackCatalogItem item = request.getCatalogItem();
        String name = item != null ? item.getName() : "";
        String body = name + " " + request.getQuantity() + "개";
        notificationService.createNotification(
                request.getUser(), NotificationType.REQUEST, PURCHASE_TITLE, body,
                null, null, RequestKind.SNACK);
    }

    private void notifyRejected(SnackRequest request) {
        SnackCatalogItem item = request.getCatalogItem();
        String name = item != null ? item.getName() : "";
        RequestRejectReason reason = request.getRejectReason();
        String label = reason != null ? reason.getLabel() : "";
        StringBuilder body = new StringBuilder();
        body.append(name).append(" · ").append(label);
        if (reason == RequestRejectReason.OTHER && request.getRejectNote() != null) {
            body.append(" · ").append(request.getRejectNote());
        }
        notificationService.createNotification(
                request.getUser(), NotificationType.REQUEST, REJECT_TITLE, body.toString(),
                null, null, RequestKind.SNACK);
    }

    private Pageable defaultDisplayOrder(Pageable pageable) {
        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Order.asc("displayOrder")));
        }
        return pageable;
    }

    private Map<Long, Integer> pendingQtyByItem() {
        Map<Long, Integer> map = new HashMap<>();
        List<Object[]> rows = snackRequestRepository.sumPendingByItem();
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            int qty = row[1] == null ? 0 : ((Number) row[1]).intValue();
            map.put(id, qty);
        }
        return map;
    }

    private Map<Long, SnackRequest> myPendingByItem(Long userId) {
        Map<Long, SnackRequest> map = new HashMap<>();
        List<SnackRequest> mine = snackRequestRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, RequestStatus.PENDING);
        if (mine == null) {
            return map;
        }
        for (SnackRequest r : mine) {
            if (r.getCatalogItem() != null) {
                map.put(r.getCatalogItem().getId(), r);
            }
        }
        return map;
    }

    private SnackCatalogCardResponse toCard(SnackCatalogItem item,
                                            Map<Long, Integer> pendingByItem,
                                            Map<Long, SnackRequest> mineByItem) {
        SnackRequest mine = mineByItem.get(item.getId());
        int pendingQty = pendingByItem.getOrDefault(item.getId(), 0);
        int myPendingQty = mine == null ? 0 : mine.getQuantity();
        Long myRequestId = mine == null ? null : mine.getId();
        boolean canRequest = item.isAvailable() && !item.isHiddenFromUsers() && myPendingQty == 0;

        SnackCatalogCardResponse response = new SnackCatalogCardResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setImageUrl(item.getImageUrl());
        response.setSsgUrl(SsgSnackCatalogParser.itemViewUrl(item.getSsgItemId()));
        if (item.getCategory() != null) {
            response.setCategory(item.getCategory().name());
            response.setCategoryLabel(item.getCategory().getLabel());
        }
        response.setPendingQty(pendingQty);
        response.setMyPendingQty(myPendingQty);
        response.setMyRequestId(myRequestId);
        response.setAvailable(item.isAvailable());
        response.setHiddenFromUsers(item.isHiddenFromUsers());
        response.setCanRequest(canRequest);
        return response;
    }

    private AdminSnackCatalogItemResponse toAdminItem(SnackCatalogItem item, Map<Long, Integer> pendingByItem) {
        AdminSnackCatalogItemResponse response = new AdminSnackCatalogItemResponse();
        response.setId(item.getId());
        response.setSsgItemId(item.getSsgItemId());
        if (item.getCategory() != null) {
            response.setCategory(item.getCategory().name());
            response.setCategoryLabel(item.getCategory().getLabel());
        }
        response.setName(item.getName());
        response.setImageUrl(item.getImageUrl());
        response.setSsgUrl(SsgSnackCatalogParser.itemViewUrl(item.getSsgItemId()));
        response.setSsgImageUrl(item.getSsgImageUrl());
        response.setDisplayOrder(item.getDisplayOrder());
        response.setAvailable(item.isAvailable());
        response.setHiddenFromUsers(item.isHiddenFromUsers());
        response.setHiddenLockedByAdmin(item.isHiddenLockedByAdmin());
        response.setLastSeenAt(item.getLastSeenAt());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        response.setPendingQty(pendingByItem.getOrDefault(item.getId(), 0));
        return response;
    }

    private static String iso(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
