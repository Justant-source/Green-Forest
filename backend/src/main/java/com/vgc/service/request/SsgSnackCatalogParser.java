package com.vgc.service.request;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SsgSnackCatalogParser {

    private static final String ORIGIN = "https://www.ssg.com";
    private static final Pattern ITEM_ID = Pattern.compile("\"itemId\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern ITEM_NM = stringField("itemNm");
    private static final Pattern ITEM_NAME = stringField("itemName");
    private static final Pattern ITEM_LNKD = stringField("itemLnkd");
    private static final Pattern ITEM_URL = stringField("itemUrl");
    private static final Pattern ITEM_IMG_URL = stringField("itemImgUrl");
    private static final Pattern IMG_URL = stringField("imgUrl");
    private static final Pattern ITEM_IMG = stringField("itemImg");

    public List<SsgParsedItem> parse(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Map<String, SsgParsedItem> byId = new LinkedHashMap<>();
        Matcher idMatcher = ITEM_ID.matcher(html);
        while (idMatcher.find()) {
            String itemId = idMatcher.group(1);
            if (byId.containsKey(itemId)) {
                continue;
            }
            String object = objectAround(html, idMatcher.start(), idMatcher.end());
            String name = firstNonBlank(firstField(ITEM_NM, object), firstField(ITEM_NAME, object));
            if (name == null || name.isBlank()) {
                continue;
            }
            String link = firstNonBlank(firstField(ITEM_LNKD, object), firstField(ITEM_URL, object));
            String image = firstNonBlank(
                    firstField(ITEM_IMG_URL, object),
                    firstField(IMG_URL, object),
                    firstField(ITEM_IMG, object));
            String ssgUrl = desktopItemUrl(link, itemId);
            byId.put(itemId, new SsgParsedItem(itemId, name, ssgUrl, absolutizeNullable(image)));
        }
        return new ArrayList<>(byId.values());
    }

    private static Pattern stringField(String key) {
        return Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    }

    private static String objectAround(String html, int idStart, int idEnd) {
        int braceStart = html.lastIndexOf('{', idStart);
        int braceEnd = html.indexOf('}', idEnd);
        if (braceStart >= 0 && braceEnd > braceStart) {
            return html.substring(braceStart, braceEnd + 1);
        }
        int start = Math.max(0, idStart - 200);
        int end = Math.min(html.length(), idEnd + 400);
        return html.substring(start, end);
    }

    private static String firstField(Pattern pattern, String window) {
        Matcher matcher = pattern.matcher(window);
        return matcher.find() ? unescapeJson(matcher.group(1)) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String absolutizeNullable(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return absolutize(url);
    }

    private static String desktopItemUrl(String link, String itemId) {
        if (link == null || link.isBlank()) {
            return ORIGIN + "/item/itemView.ssg?itemId=" + itemId;
        }
        String absolute = absolutize(link);
        return absolute.replace("https://m.ssg.com", ORIGIN).replace("http://m.ssg.com", ORIGIN);
    }

    private static String absolutize(String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        if (!trimmed.startsWith("/")) {
            return ORIGIN + "/" + trimmed;
        }
        return ORIGIN + trimmed;
    }

    private static String unescapeJson(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                out.append(c);
                continue;
            }
            char n = raw.charAt(++i);
            switch (n) {
                case '"':
                case '\\':
                case '/':
                    out.append(n);
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 't':
                    out.append('\t');
                    break;
                default:
                    out.append(n);
                    break;
            }
        }
        return out.toString();
    }
}
