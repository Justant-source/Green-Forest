package com.vgc.service.request;

import com.vgc.entity.NotificationType;
import com.vgc.entity.RequestKind;
import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.RequestStatus;
import com.vgc.entity.SnackCatalogItem;
import com.vgc.entity.SnackRequest;
import com.vgc.entity.User;
import com.vgc.repository.SnackCatalogItemRepository;
import com.vgc.repository.SnackRequestRepository;
import com.vgc.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnackRequestAdminServiceTest {

    @Mock SnackCatalogItemRepository catalogRepo;
    @Mock SnackRequestRepository requestRepo;
    @Mock NotificationService notificationService;

    @InjectMocks
    SnackRequestService sut;

    User user;
    User other;
    SnackCatalogItem visible;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setNickname("u");
        other = new User();
        other.setId(3L);
        other.setNickname("o");
        visible = new SnackCatalogItem();
        visible.setId(10L);
        visible.setName("새우깡");
        visible.setAvailable(true);
        visible.setHiddenFromUsers(false);
    }

    @Test
    void OTHER_공란_거절은_400() {
        SnackRequest pending = pendingRequest(1L, user, 1);
        when(requestRepo.findById(1L)).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> sut.rejectOne(1L, RequestRejectReason.OTHER, "  "))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("기타는 사유를 입력하세요.");
        assertThatThrownBy(() -> sut.rejectOne(1L, RequestRejectReason.OTHER, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("기타는 사유를 입력하세요.");
    }

    @Test
    void POLICY_거절은_note를_저장하지_않음() {
        SnackRequest pending = pendingRequest(1L, user, 1);
        when(requestRepo.findById(1L)).thenReturn(Optional.of(pending));
        when(requestRepo.save(any(SnackRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        sut.rejectOne(1L, RequestRejectReason.POLICY, "관리자 메모");

        ArgumentCaptor<SnackRequest> captor = ArgumentCaptor.forClass(SnackRequest.class);
        verify(requestRepo).save(captor.capture());
        assertThat(captor.getValue().getRejectNote()).isNull();
        assertThat(captor.getValue().getRejectReason()).isEqualTo(RequestRejectReason.POLICY);
        assertThat(captor.getValue().getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void 묶음_구매시_PENDING_2건_알림_2() {
        SnackRequest first = pendingRequest(1L, user, 1);
        SnackRequest second = pendingRequest(2L, other, 2);
        when(requestRepo.findByCatalogItemIdAndStatus(10L, RequestStatus.PENDING))
            .thenReturn(List.of(first, second));
        when(requestRepo.save(any(SnackRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        int updated = sut.purchaseAllPendingForItem(10L);

        assertThat(updated).isEqualTo(2);
        verify(requestRepo, times(2)).save(any(SnackRequest.class));
        verify(notificationService, times(2)).createNotification(
            any(User.class),
            eq(NotificationType.REQUEST),
            eq("간식 구매 완료"),
            anyString(),
            isNull(),
            isNull(),
            eq(RequestKind.SNACK));
    }

    @Test
    void purchaseOne_이미_PURCHASED면_409() {
        SnackRequest done = pendingRequest(1L, user, 1);
        done.setStatus(RequestStatus.PURCHASED);
        when(requestRepo.findById(1L)).thenReturn(Optional.of(done));

        assertThatThrownBy(() -> sut.purchaseOne(1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void 빈_묶음_구매는_404() {
        when(requestRepo.findByCatalogItemIdAndStatus(10L, RequestStatus.PENDING))
            .thenReturn(List.of());

        assertThatThrownBy(() -> sut.purchaseAllPendingForItem(10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("대기 중인 요청이 없습니다.")
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    private SnackRequest pendingRequest(Long id, User owner, int quantity) {
        SnackRequest request = new SnackRequest();
        request.setId(id);
        request.setUser(owner);
        request.setCatalogItem(visible);
        request.setQuantity(quantity);
        request.setStatus(RequestStatus.PENDING);
        return request;
    }
}
