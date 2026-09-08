package com.vgc.controller;

import com.vgc.dto.request.SnackCatalogCardResponse;
import com.vgc.dto.request.SnackCreateRequest;
import com.vgc.dto.request.SnackQuantityRequest;
import com.vgc.dto.request.SnackRequestResponse;
import com.vgc.dto.request.SnackSsgCategoryResponse;
import com.vgc.dto.request.SupplyCreateRequest;
import com.vgc.dto.request.SupplyRequestResponse;
import com.vgc.dto.request.SupplyUpdateRequest;
import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.SnackSsgCategory;
import com.vgc.entity.SupplyRequest;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.request.SnackRequestService;
import com.vgc.service.request.SupplyRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/requests")
public class RequestController {

    private final SnackRequestService snackRequestService;
    private final SupplyRequestService supplyRequestService;
    private final UserRepository userRepository;

    public RequestController(SnackRequestService snackRequestService,
                             SupplyRequestService supplyRequestService,
                             UserRepository userRepository) {
        this.snackRequestService = snackRequestService;
        this.supplyRequestService = supplyRequestService;
        this.userRepository = userRepository;
    }

    @GetMapping("/snacks/categories")
    public List<SnackSsgCategoryResponse> listSnackCategories(Authentication authentication) {
        requireUser(authentication);
        return snackRequestService.listVisibleCategories();
    }

    @GetMapping("/snacks/catalog")
    public Page<SnackCatalogCardResponse> listSnackCatalog(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "false") boolean requestedOnly,
            @RequestParam(defaultValue = "SNACK") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size,
            Authentication authentication) {
        User user = requireUser(authentication);
        return snackRequestService.listCatalog(
                user, q, requestedOnly, parseCategory(category), catalogPage(page, size));
    }

    @GetMapping("/snacks/mine")
    public List<SnackRequestResponse> listMySnackRequests(Authentication authentication) {
        return snackRequestService.listMine(requireUser(authentication));
    }

    @PostMapping("/snacks")
    public ResponseEntity<SnackRequestResponse> createSnackRequest(
            @RequestBody SnackCreateRequest body,
            Authentication authentication) {
        User user = requireUser(authentication);
        SnackRequestResponse response = snackRequestService.toResponse(
                snackRequestService.create(user, body.getCatalogItemId(), body.getQuantity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/snacks/{id}")
    public SnackRequestResponse updateSnackQuantity(
            @PathVariable Long id,
            @RequestBody SnackQuantityRequest body,
            Authentication authentication) {
        User user = requireUser(authentication);
        return snackRequestService.toResponse(
                snackRequestService.updateQuantity(user, id, body.getQuantity()));
    }

    @DeleteMapping("/snacks/{id}")
    public ResponseEntity<Void> cancelSnackRequest(@PathVariable Long id, Authentication authentication) {
        snackRequestService.cancel(requireUser(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/supplies/mine")
    public List<SupplyRequestResponse> listMySupplyRequests(Authentication authentication) {
        return supplyRequestService.listMine(requireUser(authentication)).stream()
                .map(this::toSupplyResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/supplies")
    public ResponseEntity<SupplyRequestResponse> createSupplyRequest(
            @RequestBody SupplyCreateRequest body,
            Authentication authentication) {
        User user = requireUser(authentication);
        SupplyRequest created = supplyRequestService.create(
                user, body.getTitle(), body.getQuantity(), body.getMemo());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSupplyResponse(created));
    }

    @PatchMapping("/supplies/{id}")
    public SupplyRequestResponse updateSupplyRequest(
            @PathVariable Long id,
            @RequestBody SupplyUpdateRequest body,
            Authentication authentication) {
        User user = requireUser(authentication);
        return toSupplyResponse(supplyRequestService.update(
                user, id, body.getTitle(), body.getQuantity(), body.getMemo()));
    }

    @DeleteMapping("/supplies/{id}")
    public ResponseEntity<Void> cancelSupplyRequest(@PathVariable Long id, Authentication authentication) {
        supplyRequestService.cancel(requireUser(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private User requireUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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

    private SupplyRequestResponse toSupplyResponse(SupplyRequest request) {
        RequestRejectReason reason = request.getRejectReason();
        SupplyRequestResponse response = new SupplyRequestResponse();
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
        return response;
    }

    private static String iso(java.time.LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
