package com.vgc.service.request;

import com.vgc.dto.request.CatalogSyncStatusResponse;
import com.vgc.entity.CatalogSyncRun;
import com.vgc.entity.CatalogSyncStatus;
import com.vgc.entity.SnackCatalogItem;
import com.vgc.entity.SnackSsgCategory;
import com.vgc.entity.User;
import com.vgc.repository.CatalogSyncRunRepository;
import com.vgc.repository.SnackCatalogItemRepository;
import com.vgc.service.ImageStorageService;
import com.vgc.util.AppTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SnackCatalogSyncService {

    static final String ZERO_ITEMS_MESSAGE =
            "SSG에서 상품을 읽지 못했습니다. 페이지 구조가 바뀌었을 수 있습니다.";

    private final CatalogSyncRunRepository syncRunRepository;
    private final SnackCatalogItemRepository catalogItemRepository;
    private final SsgSnackCatalogClient client;
    private final ImageStorageService imageStorageService;

    public SnackCatalogSyncService(CatalogSyncRunRepository syncRunRepository,
                                   SnackCatalogItemRepository catalogItemRepository,
                                   SsgSnackCatalogClient client,
                                   ImageStorageService imageStorageService) {
        this.syncRunRepository = syncRunRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.client = client;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public CatalogSyncRun sync(User admin) {
        if (syncRunRepository.existsByStatus(CatalogSyncStatus.RUNNING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 목록 갱신이 진행 중입니다.");
        }

        CatalogSyncRun run = new CatalogSyncRun();
        run.setStatus(CatalogSyncStatus.RUNNING);
        run.setStartedAt(AppTime.nowKst());
        run.setStartedBy(admin);
        run = syncRunRepository.save(run);

        List<SsgParsedItem> parsed;
        try {
            parsed = client.fetchAllPages();
        } catch (RuntimeException e) {
            fail(run, truncate(e.getMessage()));
            return run;
        }

        if (parsed == null || parsed.isEmpty()) {
            fail(run, ZERO_ITEMS_MESSAGE);
            return run;
        }

        LocalDateTime seenAt = AppTime.nowKst();
        Map<SnackSsgCategory, Set<String>> idsByCategory = new EnumMap<>(SnackSsgCategory.class);
        Map<SnackSsgCategory, Integer> orderByCategory = new EnumMap<>(SnackSsgCategory.class);
        for (SsgParsedItem item : parsed) {
            SnackSsgCategory category = item.getCategory() == null ? SnackSsgCategory.SNACK : item.getCategory();
            int order = orderByCategory.getOrDefault(category, 0);
            upsert(item, category, order, seenAt);
            orderByCategory.put(category, order + 1);
            idsByCategory.computeIfAbsent(category, k -> new HashSet<>()).add(item.getSsgItemId());
        }

        for (Map.Entry<SnackSsgCategory, Set<String>> entry : idsByCategory.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            for (SnackCatalogItem existing : catalogItemRepository.findByCategory(entry.getKey())) {
                if (!entry.getValue().contains(existing.getSsgItemId())) {
                    existing.setAvailable(false);
                    catalogItemRepository.save(existing);
                }
            }
        }

        run.setStatus(CatalogSyncStatus.SUCCESS);
        run.setFinishedAt(AppTime.nowKst());
        run.setItemCount(parsed.size());
        run.setErrorMessage(null);
        return syncRunRepository.save(run);
    }

    public CatalogSyncStatusResponse getStatus() {
        CatalogSyncStatusResponse response = new CatalogSyncStatusResponse();
        response.setRunning(syncRunRepository.existsByStatus(CatalogSyncStatus.RUNNING));

        CatalogSyncRun lastSuccess = syncRunRepository
                .findFirstByStatusOrderByFinishedAtDesc(CatalogSyncStatus.SUCCESS)
                .orElse(null);
        if (lastSuccess != null) {
            response.setLastSuccessAt(formatTime(lastSuccess.getFinishedAt()));
            response.setItemCount(lastSuccess.getItemCount());
        }

        CatalogSyncRun lastFail = syncRunRepository
                .findFirstByStatusOrderByFinishedAtDesc(CatalogSyncStatus.FAIL)
                .orElse(null);
        if (lastFail != null && (lastSuccess == null || lastFail.getFinishedAt() != null
                && lastSuccess.getFinishedAt() != null
                && lastFail.getFinishedAt().isAfter(lastSuccess.getFinishedAt()))) {
            response.setLastFailAt(formatTime(lastFail.getFinishedAt()));
            response.setLastError(lastFail.getErrorMessage());
        }
        return response;
    }

    @Transactional
    public void setVisibility(Long catalogItemId, boolean hiddenFromUsers) {
        SnackCatalogItem item = catalogItemRepository.findById(catalogItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        item.setHiddenFromUsers(hiddenFromUsers);
        item.setHiddenLockedByAdmin(true);
        catalogItemRepository.save(item);
    }

    @Transactional
    public void setCategoryVisibility(SnackSsgCategory category, boolean hiddenFromUsers) {
        for (SnackCatalogItem item : catalogItemRepository.findByCategory(category)) {
            item.setHiddenFromUsers(hiddenFromUsers);
            item.setHiddenLockedByAdmin(true);
            catalogItemRepository.save(item);
        }
    }

    private void upsert(SsgParsedItem parsed, SnackSsgCategory category, int displayOrder, LocalDateTime seenAt) {
        SnackCatalogItem item = catalogItemRepository
                .findBySsgItemIdAndCategory(parsed.getSsgItemId(), category)
                .orElseGet(SnackCatalogItem::new);
        boolean isNew = item.getSsgItemId() == null;
        if (isNew) {
            item.setSsgItemId(parsed.getSsgItemId());
            item.setCategory(category);
            item.setHiddenLockedByAdmin(false);
        }
        item.setName(parsed.getName());
        item.setSsgUrl(parsed.getSsgUrl());
        item.setSsgImageUrl(parsed.getImageUrl());
        item.setDisplayOrder(displayOrder);
        item.setAvailable(true);
        item.setLastSeenAt(seenAt);
        if (!item.isHiddenLockedByAdmin()) {
            item.setHiddenFromUsers(SnackHideRules.matches(parsed.getName()));
        }
        item.setImageUrl(resolveStoredImage(item.getImageUrl(), parsed.getImageUrl()));
        catalogItemRepository.save(item);
    }

    private String resolveStoredImage(String existingStoredUrl, String remoteUrl) {
        if (existingStoredUrl != null && !existingStoredUrl.isBlank()) {
            return existingStoredUrl;
        }
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = client.getBytes(remoteUrl);
            return imageStorageService.uploadBytes(bytes, filenameFromUrl(remoteUrl));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private void fail(CatalogSyncRun run, String message) {
        run.setStatus(CatalogSyncStatus.FAIL);
        run.setFinishedAt(AppTime.nowKst());
        run.setErrorMessage(truncate(message));
        syncRunRepository.save(run);
    }

    private static String formatTime(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private static String filenameFromUrl(String url) {
        int q = url.indexOf('?');
        String path = q >= 0 ? url.substring(0, q) : url;
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.isBlank() ? "ssg.jpg" : name;
    }
}
