package com.vgc.controller;

import com.vgc.dto.request.AdminSnackCatalogItemResponse;
import com.vgc.dto.request.AdminSnackRequestResponse;
import com.vgc.dto.request.CatalogSyncStatusResponse;
import com.vgc.dto.request.PurchaseCountResponse;
import com.vgc.dto.request.RejectRequestBody;
import com.vgc.dto.request.SnackGroupedPendingResponse;
import com.vgc.dto.request.SnackSsgCategoryResponse;
import com.vgc.dto.request.SupplyAdminResponse;
import com.vgc.dto.request.VisibilityRequest;
import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.RequestStatus;
import com.vgc.entity.SnackSsgCategory;
import com.vgc.entity.SupplyRequest;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.request.SnackCatalogSyncService;
import com.vgc.service.request.SnackRequestService;
import com.vgc.service.request.SupplyRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/requests")
public class AdminRequestController {

    private final SnackRequestService snackRequestService;
    private final SupplyRequestService supplyRequestService;
    private final SnackCatalogSyncService snackCatalogSyncService;
    private final UserRepository userRepository;

    public AdminRequestController(SnackRequestService snackRequestService,
                                  SupplyRequestService supplyRequestService,
                                  SnackCatalogSyncService snackCatalogSyncService,
                                  UserRepository userRepository) {
        this.snackRequestService = snackRequestService;
        this.supplyRequestService = supplyRequestService;
        this.snackCatalogSyncService = snackCatalogSyncService;
        this.userRepository = userRepository;
    }

    @GetMapping("/snacks/grouped")
    public List<SnackGroupedPendingResponse> listGroupedSnacks(Authentication authentication) {
        requireAdmin(authentication);
        return snackRequestService.listGroupedPending();
    }

    @GetMapping("/snacks/history")
    public List<AdminSnackRequestResponse> listSnackHistory(Authentication authentication) {
        requireAdmin(authentication);
        return snackRequestService.listHistory();
    }

    @PostMapping("/snacks/items/{catalogItemId}/purchase")
    public PurchaseCountResponse purchaseSnackItem(
            @PathVariable Long catalogItemId,
            Authentication authentication) {
        requireAdmin(authentication);
        return new PurchaseCountResponse(snackRequestService.purchaseAllPendingForItem(catalogItemId));
    }

    @PostMapping("/snacks/{id}/purchase")
    public void purchaseSnackRequest(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        snackRequestService.purchaseOne(id);
    }

    @PostMapping("/snacks/{id}/reject-purchased")
    public void rejectPurchasedSnackRequest(
            @PathVariable Long id,
            @RequestBody RejectRequestBody body,
            Authentication authentication) {
        requireAdmin(authentication);
        RequestRejectReason reason = requireReason(body);
        snackRequestService.rejectPurchased(id, reason, body.getNote());
    }

    @PostMapping("/snacks/items/{catalogItemId}/reject")
    public PurchaseCountResponse rejectSnackItem(
            @PathVariable Long catalogItemId,
            @RequestBody RejectRequestBody body,
            Authentication authentication) {
        requireAdmin(authentication);
        RequestRejectReason reason = requireReason(body);
        return new PurchaseCountResponse(
                snackRequestService.rejectAllPendingForItem(catalogItemId, reason, body.getNote()));
    }

    @PostMapping("/snacks/{id}/reject")
    public void rejectSnackRequest(
            @PathVariable Long id,
            @RequestBody RejectRequestBody body,
            Authentication authentication) {
        requireAdmin(authentication);
        RequestRejectReason reason = requireReason(body);
        snackRequestService.rejectOne(id, reason, body.getNote());
    }

    @GetMapping("/supplies")
    public List<SupplyAdminResponse> listSupplies(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        requireAdmin(authentication);
        return supplyRequestService.listForAdmin(parseStatus(status)).stream()
                .map(this::toAdminSupply)
                .collect(Collectors.toList());
    }

    @GetMapping("/supplies/history")
    public List<SupplyAdminResponse> listSupplyHistory(Authentication authentication) {
        requireAdmin(authentication);
        return supplyRequestService.listHistory().stream()
                .map(this::toAdminSupply)
                .collect(Collectors.toList());
    }

    @PostMapping("/supplies/{id}/purchase")
    public void purchaseSupplyRequest(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        supplyRequestService.purchase(id);
    }

    @PostMapping("/supplies/{id}/reject")
    public void rejectSupplyRequest(
            @PathVariable Long id,
            @RequestBody RejectRequestBody body,
            Authentication authentication) {
        requireAdmin(authentication);
        RequestRejectReason reason = requireReason(body);
        supplyRequestService.reject(id, reason, body.getNote());
    }

    @GetMapping("/catalog/categories")
    public List<SnackSsgCategoryResponse> listCatalogCategories(Authentication authentication) {
        requireAdmin(authentication);
        return snackRequestService.listCategories();
    }

    @GetMapping("/catalog")
    public Page<AdminSnackCatalogItemResponse> listAdminCatalog(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "true") boolean includeUnavailable,
            @RequestParam(defaultValue = "SNACK") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size,
            Authentication authentication) {
        requireAdmin(authentication);
        return snackRequestService.listAdminCatalog(
                q, includeUnavailable, parseCategory(category), catalogPage(page, size));
    }

    @PatchMapping("/catalog/{id}/visibility")
    public void setCatalogVisibility(
            @PathVariable Long id,
            @RequestBody VisibilityRequest body,
            Authentication authentication) {
        requireAdmin(authentication);
        snackCatalogSyncService.setVisibility(id, body.isHiddenFromUsers());
    }

    @PatchMapping("/catalog/categories/{category}/visibility")
    public void setCategoryVisibility(
            @PathVariable String category,
            @RequestBody VisibilityRequest body,
            Authentication authentication) {
        requireAdmin(authentication);
        snackCatalogSyncService.setCategoryVisibility(parseCategory(category), body.isHiddenFromUsers());
    }

    @PostMapping("/catalog/sync")
    public CatalogSyncStatusResponse syncCatalog(Authentication authentication) {
        User admin = requireAdmin(authentication);
        snackCatalogSyncService.sync(admin);
        return snackCatalogSyncService.getStatus();
    }

    @GetMapping("/catalog/sync/status")
    public CatalogSyncStatusResponse catalogSyncStatus(Authentication authentication) {
        requireAdmin(authentication);
        return snackCatalogSyncService.getStatus();
    }

    private User requireAdmin(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
        return user;
    }

    private Pageable catalogPage(int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(pageNumber, pageSize);
    }

    private static SnackSsgCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return SnackSsgCategory.SNACK;
        }
        try {
            return SnackSsgCategory.valueOf(category);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않는 분류입니다.");
        }
    }

    private RequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return RequestStatus.PENDING;
        }
        try {
            return RequestStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않는 상태입니다.");
        }
    }

    private RequestRejectReason requireReason(RejectRequestBody body) {
        if (body == null || body.getReason() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거절 사유를 선택하세요.");
        }
        return body.getReason();
    }

    private SupplyAdminResponse toAdminSupply(SupplyRequest request) {
        RequestRejectReason reason = request.getRejectReason();
        User user = request.getUser();
        SupplyAdminResponse response = new SupplyAdminResponse();
        response.setId(request.getId());
        response.setTitle(request.getTitle());
        response.setQuantity(request.getQuantity());
        response.setMemo(request.getMemo());
        response.setStatus(request.getStatus());
        response.setRejectReason(reason);
        response.setRejectReasonLabel(reason != null ? reason.getLabel() : null);
        response.setRejectNote(request.getRejectNote());
        response.setCreatedAt(iso(request.getCreatedAt()));
        response.setPurchasedAt(iso(request.getPurchasedAt()));
        response.setRejectedAt(iso(request.getRejectedAt()));
        response.setUserId(user != null ? user.getId() : null);
        response.setNickname(user != null ? user.getNickname() : null);
        return response;
    }

    private static String iso(java.time.LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
