package com.recipehub.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String text) {
        String s = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");
        return s.replaceAll("^-|-$", "");
    }

    // ponytail: dò tuần tự base-2, base-3…; đủ cho catalog vài trăm dòng
    public static String unique(String base, Predicate<String> exists) {
        String slug = base;
        for (int i = 2; exists.test(slug); i++) slug = base + "-" + i;
        return slug;
    }
}
