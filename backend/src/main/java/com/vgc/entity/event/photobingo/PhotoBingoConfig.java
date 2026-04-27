package com.vgc.entity.event.photobingo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PhotoBingo 설정 POJO — Event.configJson 에 JSON 직렬화되어 저장된다.
 *
 * equals/hashCode 필수: 이 POJO 는 {@code Event.configJson} 에 AttributeConverter 로 매핑되며,
 * Hibernate 의 dirty-check 가 값 비교(Objects.equals)로 이루어진다. 이 메서드가 없으면
 * Object 기본 레퍼런스 비교로 떨어져 매 로드마다 false dirty 판정 → spurious UPDATE events →
 * outer TX X 락 → REQUIRES_NEW INSERT 가 같은 요청 내에서 self-block 되는 버그가 재발한다.
 */
public class PhotoBingoConfig {

    private List<String> themes = new ArrayList<>();
    private Rewards rewards = new Rewards();

    public List<String> getThemes() { return themes; }
    public void setThemes(List<String> themes) { this.themes = themes; }
    public Rewards getRewards() { return rewards; }
    public void setRewards(Rewards rewards) { this.rewards = rewards; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhotoBingoConfig that)) return false;
        return Objects.equals(themes, that.themes) && Objects.equals(rewards, that.rewards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(themes, rewards);
    }

    public static class Rewards {
        private int line3 = 50;
        private int line5 = 80;
        private int blackout = 120;

        public int getLine3() { return line3; }
        public void setLine3(int line3) { this.line3 = line3; }
        public int getLine5() { return line5; }
        public void setLine5(int line5) { this.line5 = line5; }
        public int getBlackout() { return blackout; }
        public void setBlackout(int blackout) { this.blackout = blackout; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Rewards that)) return false;
            return line3 == that.line3 && line5 == that.line5 && blackout == that.blackout;
        }

        @Override
        public int hashCode() {
            return Objects.hash(line3, line5, blackout);
        }
    }
}
