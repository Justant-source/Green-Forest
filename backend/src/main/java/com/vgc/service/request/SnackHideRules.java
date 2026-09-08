package com.vgc.service.request;

import java.util.List;
import java.util.Locale;

public final class SnackHideRules {
    public static final List<String> KEYWORDS = List.of(
        "냉동", "아이스크림", "아이스", "빙과", "frozen"
    );

    private SnackHideRules() {
    }

    public static boolean matches(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String keyword : KEYWORDS) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
