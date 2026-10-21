package com.vgc.service.request;

import com.vgc.entity.CatalogSyncRun;
import com.vgc.entity.CatalogSyncStatus;
import com.vgc.entity.SnackCatalogItem;
import com.vgc.entity.SnackSsgCategory;
import com.vgc.entity.User;
import com.vgc.repository.CatalogSyncRunRepository;
import com.vgc.repository.SnackCatalogItemRepository;
import com.vgc.service.ImageStorageService;
import com.vgc.util.AppTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnackCatalogSyncServiceTest {

    @Mock
    private CatalogSyncRunRepository syncRunRepository;
    @Mock
    private SnackCatalogItemRepository catalogItemRepository;
    @Mock
    private SsgSnackCatalogClient client;
    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private SnackCatalogSyncService sut;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setNickname("admin");
        admin.setRole("ADMIN");
    }

    @Test
    void 파싱_0건이면_FAIL이고_카탈로그를_저장하지_않는다() {
        when(syncRunRepository.existsByStatus(CatalogSyncStatus.RUNNING)).thenReturn(false);
        when(syncRunRepository.save(any(CatalogSyncRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(client.fetchAllPages()).thenReturn(List.of());

        CatalogSyncRun result = sut.sync(admin);

        assertThat(result.getStatus()).isEqualTo(CatalogSyncStatus.FAIL);
        assertThat(result.getErrorMessage())
                .isEqualTo("SSG에서 상품을 읽지 못했습니다. 페이지 구조가 바뀌었을 수 있습니다.");
        verify(catalogItemRepository, never()).save(any());
        verify(catalogItemRepository, never()).findByCategory(any());
    }

    @Test
    void 관리자_잠금_숨김은_유지한다() {
        stubSyncStart();
        SnackCatalogItem existing = new SnackCatalogItem();
        existing.setSsgItemId("100002");
        existing.setCategory(SnackSsgCategory.SNACK);
        existing.setName("이전 이름");
        existing.setSsgUrl("https://www.ssg.com/old");
        existing.setHiddenFromUsers(false);
        existing.setHiddenLockedByAdmin(true);
        existing.setAvailable(true);

        when(client.fetchAllPages()).thenReturn(List.of(
                new SsgParsedItem("100002", "하겐다즈 아이스크림",
                        "https://www.ssg.com/item/itemView.ssg?itemId=100002", null)));
        when(catalogItemRepository.findBySsgItemIdAndCategory("100002", SnackSsgCategory.SNACK))
                .thenReturn(Optional.of(existing));
        when(catalogItemRepository.findByCategory(SnackSsgCategory.SNACK)).thenReturn(List.of(existing));
        when(catalogItemRepository.save(any(SnackCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogSyncRun result = sut.sync(admin);

        assertThat(result.getStatus()).isEqualTo(CatalogSyncStatus.SUCCESS);
        assertThat(existing.isHiddenFromUsers()).isFalse();
        assertThat(existing.isHiddenLockedByAdmin()).isTrue();
        assertThat(existing.getName()).isEqualTo("하겐다즈 아이스크림");
        assertThat(existing.isAvailable()).isTrue();
    }

    @Test
    void 목록에서_사라진_상품은_판매불가와_함께_숨김_처리한다() {
        stubSyncStart();
        SnackCatalogItem gone = new SnackCatalogItem();
        gone.setSsgItemId("100009");
        gone.setCategory(SnackSsgCategory.SNACK);
        gone.setName("사라진 과자");
        gone.setSsgUrl("https://www.ssg.com/gone");
        gone.setAvailable(true);
        gone.setHiddenFromUsers(false);
        gone.setHiddenLockedByAdmin(true);

        when(client.fetchAllPages()).thenReturn(List.of(
                new SsgParsedItem("100001", "새우깡",
                        "https://www.ssg.com/item/itemView.ssg?itemId=100001", null)));
        when(catalogItemRepository.findBySsgItemIdAndCategory("100001", SnackSsgCategory.SNACK))
                .thenReturn(Optional.empty());
        when(catalogItemRepository.findByCategory(SnackSsgCategory.SNACK)).thenReturn(List.of(gone));
        when(catalogItemRepository.save(any(SnackCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        sut.sync(admin);

        assertThat(gone.isAvailable()).isFalse();
        assertThat(gone.isHiddenFromUsers()).isTrue();
    }

    @Test
    void 잠금_없으면_아이스크림_이름은_숨긴다() {
        stubSyncStart();
        when(client.fetchAllPages()).thenReturn(List.of(
                new SsgParsedItem("100002", "하겐다즈 아이스크림",
                        "https://www.ssg.com/item/itemView.ssg?itemId=100002", null)));
        when(catalogItemRepository.findBySsgItemIdAndCategory("100002", SnackSsgCategory.SNACK))
                .thenReturn(Optional.empty());
        when(catalogItemRepository.findByCategory(SnackSsgCategory.SNACK)).thenReturn(List.of());
        when(catalogItemRepository.save(any(SnackCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogSyncRun result = sut.sync(admin);

        assertThat(result.getStatus()).isEqualTo(CatalogSyncStatus.SUCCESS);
        ArgumentCaptor<SnackCatalogItem> captor = ArgumentCaptor.forClass(SnackCatalogItem.class);
        verify(catalogItemRepository).save(captor.capture());
        SnackCatalogItem saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("하겐다즈 아이스크림");
        assertThat(saved.isHiddenFromUsers()).isTrue();
        assertThat(saved.isHiddenLockedByAdmin()).isFalse();
    }

    @Test
    void 최근_성공이_있어도_수동_갱신은_진행한다() {
        stubSyncStart();
        when(client.fetchAllPages()).thenReturn(List.of(
                new SsgParsedItem("100002", "새우깡",
                        "https://www.ssg.com/item/itemView.ssg?itemId=100002", null)));
        when(catalogItemRepository.findBySsgItemIdAndCategory("100002", SnackSsgCategory.SNACK))
                .thenReturn(Optional.empty());
        when(catalogItemRepository.findByCategory(SnackSsgCategory.SNACK)).thenReturn(List.of());
        when(catalogItemRepository.save(any(SnackCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogSyncRun result = sut.sync(admin);

        assertThat(result.getStatus()).isEqualTo(CatalogSyncStatus.SUCCESS);
        verify(client).fetchAllPages();
    }

    @Test
    void RUNNING이_있으면_409() {
        when(syncRunRepository.existsByStatus(CatalogSyncStatus.RUNNING)).thenReturn(true);

        assertThatThrownBy(() -> sut.sync(admin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
        verify(client, never()).fetchAllPages();
        verify(syncRunRepository, never()).save(any());
    }

    private void stubSyncStart() {
        when(syncRunRepository.existsByStatus(CatalogSyncStatus.RUNNING)).thenReturn(false);
        when(syncRunRepository.save(any(CatalogSyncRun.class))).thenAnswer(inv -> inv.getArgument(0));
    }
}
