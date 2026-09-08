package com.vgc.service.request;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnackRequestServiceTest {

    @Mock SnackCatalogItemRepository catalogRepo;
    @Mock SnackRequestRepository requestRepo;
    @Mock NotificationService notificationService;

    @InjectMocks
    SnackRequestService sut;

    User user;
    SnackCatalogItem visible;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setNickname("u");
        visible = new SnackCatalogItem();
        visible.setId(10L);
        visible.setName("새우깡");
        visible.setAvailable(true);
        visible.setHiddenFromUsers(false);
    }

    @Test
    void 수량_2는_거부() {
        when(catalogRepo.findById(10L)).thenReturn(Optional.of(visible));
        assertThatThrownBy(() -> sut.create(user, 10L, 2))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 숨김_상품_신규신청_거부() {
        visible.setHiddenFromUsers(true);
        when(catalogRepo.findById(10L)).thenReturn(Optional.of(visible));
        when(requestRepo.findByUserIdAndCatalogItemIdAndStatus(2L, 10L, RequestStatus.PENDING))
            .thenReturn(Optional.empty());
        assertThatThrownBy(() -> sut.create(user, 10L, 1))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unavailable_거부() {
        visible.setAvailable(false);
        when(catalogRepo.findById(10L)).thenReturn(Optional.of(visible));
        assertThatThrownBy(() -> sut.create(user, 10L, 1))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 이미_PENDING이면_생성_거부() {
        when(catalogRepo.findById(10L)).thenReturn(Optional.of(visible));
        when(requestRepo.findByUserIdAndCatalogItemIdAndStatus(2L, 10L, RequestStatus.PENDING))
            .thenReturn(Optional.of(new SnackRequest()));
        assertThatThrownBy(() -> sut.create(user, 10L, 1))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("이미 요청");
    }
}
