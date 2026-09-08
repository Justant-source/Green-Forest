package com.vgc.service.request;

import com.vgc.entity.RequestRejectReason;
import com.vgc.entity.RequestStatus;
import com.vgc.entity.SupplyRequest;
import com.vgc.entity.User;
import com.vgc.repository.SupplyRequestRepository;
import com.vgc.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplyRequestServiceTest {

    @Mock
    private SupplyRequestRepository supplyRequestRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SupplyRequestService sut;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setNickname("u");
    }

    @Test
    void 빈_이름은_거부() {
        assertThatThrownBy(() -> sut.create(user, "  ", 1, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("이름을 입력하세요");
                });
    }

    @Test
    void 수량_6은_거부() {
        assertThatThrownBy(() -> sut.create(user, "볼펜", 6, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void 남의_PENDING_수정은_거부() {
        User owner = new User();
        owner.setId(1L);
        SupplyRequest request = new SupplyRequest();
        request.setId(10L);
        request.setUser(owner);
        request.setStatus(RequestStatus.PENDING);
        when(supplyRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> sut.update(user, 10L, "수정", 1, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void PURCHASED_취소는_409() {
        SupplyRequest request = new SupplyRequest();
        request.setId(10L);
        request.setUser(user);
        request.setStatus(RequestStatus.PURCHASED);
        when(supplyRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> sut.cancel(user, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void OTHER_거절에_사유없으면_400() {
        SupplyRequest request = new SupplyRequest();
        request.setId(10L);
        request.setUser(user);
        request.setStatus(RequestStatus.PENDING);
        request.setTitle("볼펜");
        when(supplyRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> sut.reject(10L, RequestRejectReason.OTHER, "  "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("기타는 사유를 입력하세요");
                });
    }
}
