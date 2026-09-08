package com.vgc.service.request;

import com.vgc.entity.SnackSsgCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class SsgSnackCatalogClient {

    public static final String PAGE_PARAM = "page";
    public static final int MAX_PAGES = 50;
    public static final String USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    private final SsgSnackCatalogParser parser;
    private final HttpClient httpClient;

    @Autowired
    public SsgSnackCatalogClient(SsgSnackCatalogParser parser) {
        this(parser, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    SsgSnackCatalogClient(SsgSnackCatalogParser parser, HttpClient httpClient) {
        this.parser = parser;
        this.httpClient = httpClient;
    }

    public List<SsgParsedItem> fetchAllPages() {
        List<SsgParsedItem> all = new ArrayList<>();
        UncheckedIOException lastError = null;
        for (SnackSsgCategory category : SnackSsgCategory.values()) {
            try {
                all.addAll(fetchCategoryPages(category));
            } catch (UncheckedIOException e) {
                lastError = e;
            }
        }
        if (all.isEmpty() && lastError != null) {
            throw lastError;
        }
        return all;
    }

    private List<SsgParsedItem> fetchCategoryPages(SnackSsgCategory category) {
        List<SsgParsedItem> all = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            String html = getPage(category, page);
            List<SsgParsedItem> items = parser.parse(html);
            if (items.isEmpty()) {
                break;
            }
            int added = 0;
            for (SsgParsedItem item : items) {
                item.setCategory(category);
                if (seenIds.add(item.getSsgItemId())) {
                    all.add(item);
                    added++;
                }
            }
            if (added == 0) {
                break;
            }
        }
        return all;
    }

    byte[] getBytes(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("SSG image HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String getPage(SnackSsgCategory category, int page) {
        String url = category.themeUrl() + "&" + PAGE_PARAM + "=" + page;
        HttpRequest request = browserGet(url);
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException(ssgHttpError(response.statusCode()));
            }
            return response.body() != null ? response.body() : "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SSG 요청이 중단되었습니다", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HttpRequest browserGet(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .GET()
                .build();
    }

    private static String ssgHttpError(int status) {
        if (status == 403) {
            return "SSG가 목록 페이지 요청을 차단했습니다. 모바일 테마 주소로 재시도하거나 잠시 후 다시 눌러 주세요.";
        }
        return "SSG HTTP " + status;
    }
}
