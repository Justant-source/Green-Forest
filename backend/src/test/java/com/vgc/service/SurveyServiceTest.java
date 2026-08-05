package com.vgc.service;

import com.vgc.dto.SurveyCreateRequest;
import com.vgc.dto.SurveyOptionInput;
import com.vgc.entity.*;
import com.vgc.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock private SurveyRepository surveyRepository;
    @Mock private SurveyOptionRepository optionRepository;
    @Mock private SurveyVoteRepository voteRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private PostRepository postRepository;
    @Mock private PostImageRepository postImageRepository;

    @InjectMocks
    private SurveyService sut;

    private User admin;
    private User user;
    private Survey survey;
    private SurveyOption option;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setNickname("admin");
        admin.setRole("ADMIN");

        user = new User();
        user.setId(2L);
        user.setNickname("user");
        user.setRole("USER");

        Post post = new Post();
        post.setId(10L);
        post.setTitle("테스트 설문");
        post.setCategory("survey");
        post.setAuthor(admin);

        survey = new Survey();
        survey.setId(100L);
        survey.setPost(post);
        survey.setClosesAt(LocalDateTime.now().plusDays(7));
        survey.setAllowOptionAddByUser(true);
        survey.setAllowMultiSelect(false);

        option = new SurveyOption();
        option.setId(200L);
        option.setSurvey(survey);
        option.setOptionType(SurveyOptionType.TEXT_ONLY);
        option.setTextContent("옵션A");
    }

    // 1. 비관리자가 설문 생성 시도 → 거부
    @Test
    void createSurveyWithPost_일반유저_거부() {
        SurveyCreateRequest req = new SurveyCreateRequest();
        req.setClosesAt(LocalDateTime.now().plusDays(7));
        SurveyOptionInput o1 = textOption("옵션1");
        SurveyOptionInput o2 = textOption("옵션2");

        assertThatThrownBy(() -> sut.createSurveyWithPost(user, "제목", req, List.of(o1, o2)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("관리자만");
    }

    // 2. 옵션이 1개 → 거부
    @Test
    void createSurvey_옵션1개_거부() {
        SurveyCreateRequest req = new SurveyCreateRequest();
        req.setClosesAt(LocalDateTime.now().plusDays(7));

        Post post = new Post();
        post.setId(10L);
        when(postRepository.save(any())).thenReturn(post);

        assertThatThrownBy(() -> sut.createSurvey(post, req, List.of(textOption("옵션1"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최소 2개");
    }

    // 3. 옵션 16개 → 거부
    @Test
    void createSurvey_옵션16개_거부() {
        SurveyCreateRequest req = new SurveyCreateRequest();
        req.setClosesAt(LocalDateTime.now().plusDays(7));

        Post post = new Post();
        List<SurveyOptionInput> options = java.util.stream.IntStream.range(0, 16)
            .mapToObj(i -> textOption("옵션" + i))
            .collect(java.util.stream.Collectors.toList());

        assertThatThrownBy(() -> sut.createSurvey(post, req, options))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최대");
    }

    // 4. 종료된 설문에 투표 → 거부
    @Test
    void vote_종료된설문_거부() {
        survey.setClosesAt(LocalDateTime.now().minusDays(1));
        when(surveyRepository.findById(100L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> sut.vote(100L, 200L, user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("종료된 설문");
    }

    // 5. 사용자가 같은 옵션 투표 토글 해제
    @Test
    void vote_같은옵션_토글해제() {
        when(surveyRepository.findById(100L)).thenReturn(Optional.of(survey));
        when(optionRepository.findById(200L)).thenReturn(Optional.of(option));
        when(voteRepository.existsByUserIdAndOptionId(2L, 200L)).thenReturn(true);

        sut.vote(100L, 200L, user);

        verify(voteRepository).deleteByUserIdAndOptionId(2L, 200L);
        verify(voteRepository, never()).save(any());
    }

    // 6. 단일 선택 설문에서 다른 옵션 선택 시 기존 표 삭제
    @Test
    void vote_단일선택_기존표삭제() {
        SurveyOption optionB = new SurveyOption();
        optionB.setId(201L);
        optionB.setSurvey(survey);
        optionB.setOptionType(SurveyOptionType.TEXT_ONLY);
        optionB.setTextContent("옵션B");

        SurveyVote existingVote = new SurveyVote();
        existingVote.setId(300L);
        existingVote.setSurvey(survey);
        existingVote.setOption(option);
        existingVote.setUser(user);

        when(surveyRepository.findById(100L)).thenReturn(Optional.of(survey));
        when(optionRepository.findById(201L)).thenReturn(Optional.of(optionB));
        when(voteRepository.existsByUserIdAndOptionId(2L, 201L)).thenReturn(false);
        when(voteRepository.findBySurveyIdAndUserId(100L, 2L)).thenReturn(List.of(existingVote));

        sut.vote(100L, 201L, user);

        verify(voteRepository).deleteAll(List.of(existingVote));
        verify(voteRepository).save(any(SurveyVote.class));
    }

    // 7. 참여자 옵션 추가 - 3개 초과 시 거부
    @Test
    void addUserOption_3개초과_거부() {
        when(surveyRepository.findById(100L)).thenReturn(Optional.of(survey));
        when(optionRepository.countBySurveyId(100L)).thenReturn(5L);
        when(optionRepository.countBySurveyIdAndAddedByUserId(100L, 2L)).thenReturn(3L);

        assertThatThrownBy(() -> sut.addUserOption(100L, user, "새 옵션"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최대 3개");
    }

    // 8. 설문이 허용하지 않는 경우 사용자 옵션 추가 거부
    @Test
    void addUserOption_허용안됨_거부() {
        survey.setAllowOptionAddByUser(false);
        when(surveyRepository.findById(100L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> sut.addUserOption(100L, user, "새 옵션"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("허용되지 않습니다");
    }

    private SurveyOptionInput textOption(String text) {
        SurveyOptionInput in = new SurveyOptionInput();
        in.setType(SurveyOptionType.TEXT_ONLY);
        in.setText(text);
        return in;
    }
}
