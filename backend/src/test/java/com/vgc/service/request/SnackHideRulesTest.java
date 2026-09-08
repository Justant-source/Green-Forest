package com.vgc.service.request;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SnackHideRulesTest {
    @Test
    void 키워드가_이름에_있으면_숨김() {
        assertThat(SnackHideRules.matches("해태 아이스크림 바")).isTrue();
        assertThat(SnackHideRules.matches("냉동 만두 스낵")).isTrue();
        assertThat(SnackHideRules.matches("FROZEN 요거트")).isTrue();
        assertThat(SnackHideRules.matches("빙과 모음")).isTrue();
        assertThat(SnackHideRules.matches("아이스 초코파이")).isTrue();
    }

    @Test
    void 일반_과자는_숨기지_않음() {
        assertThat(SnackHideRules.matches("농심 새우깡")).isFalse();
        assertThat(SnackHideRules.matches(null)).isFalse();
        assertThat(SnackHideRules.matches("")).isFalse();
    }
}
