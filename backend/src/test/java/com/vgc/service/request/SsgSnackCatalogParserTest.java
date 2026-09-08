package com.vgc.service.request;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SsgSnackCatalogParserTest {

    private final SsgSnackCatalogParser parser = new SsgSnackCatalogParser();

    @Test
    void 샘플에서_상품_2건을_파싱한다() throws Exception {
        String html;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("ssg/snack-category-sample.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<SsgParsedItem> items = parser.parse(html);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getSsgItemId()).isEqualTo("1000018629198");
        assertThat(items.get(0).getName()).isEqualTo("농심 자갈치90g");
        assertThat(items.get(0).getSsgUrl())
                .isEqualTo("https://www.ssg.com/item/itemView.ssg?itemId=1000018629198");
        assertThat(items.get(0).getImageUrl())
                .isEqualTo("https://item.ssgcdn.com/98/91/86/item/1000018629198_i1_1200.jpg");
        assertThat(SnackHideRules.matches(items.get(0).getName())).isFalse();

        assertThat(items.get(1).getSsgItemId()).isEqualTo("100002");
        assertThat(items.get(1).getName()).isEqualTo("하겐다즈 아이스크림");
        assertThat(items.get(1).getSsgUrl())
                .isEqualTo("https://www.ssg.com/item/itemView.ssg?itemId=100002");
    }

    @Test
    void 빈_html은_빈_리스트() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("<html></html>")).isEmpty();
    }

    @Test
    void 모바일_itemName_필드도_파싱한다() {
        String html = "{\"itemId\":\"1000036235321\",\"itemName\":\"예감 치즈그라탕 204g\","
                + "\"itemUrl\":\"https://m.ssg.com/item/itemView.ssg?itemId=1000036235321\","
                + "\"itemImgUrl\":\"https://sitem.ssgcdn.com/x.jpg\"}";
        List<SsgParsedItem> items = parser.parse(html);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("예감 치즈그라탕 204g");
        assertThat(items.get(0).getSsgUrl())
                .isEqualTo("https://www.ssg.com/item/itemView.ssg?itemId=1000036235321");
        assertThat(items.get(0).getImageUrl()).isEqualTo("https://sitem.ssgcdn.com/x.jpg");
    }

    @Test
    void itemId_없는_블록은_건너뛴다() {
        String html = "{\"itemNm\":\"없는거\",\"itemLnkd\":\"/x\"}";
        assertThat(parser.parse(html)).isEmpty();
    }
}
