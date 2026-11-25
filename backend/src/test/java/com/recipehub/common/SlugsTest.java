package com.recipehub.common;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugsTest {

    @ParameterizedTest
    @CsvSource({
            "Cà chua, ca-chua", "Đậu phụ chiên, dau-phu-chien", "'  Phở   bò!! ', pho-bo",
            "Bánh mì & pate, banh-mi-pate", "Gà ĐÔNG TẢO, ga-dong-tao", "Nước mắm Phú Quốc, nuoc-mam-phu-quoc"})
    void slugify(String in, String out) {
        assertThat(Slugs.slugify(in)).isEqualTo(out);
    }

    @Test
    void symbolsOnlyGiveEmpty() {
        assertThat(Slugs.slugify("!!!")).isEmpty();
        assertThat(Slugs.slugify("")).isEmpty();
    }

    @Test
    void uniqueAppendsCounter() {
        assertThat(Slugs.unique("ca-chua", Set.of("ca-chua", "ca-chua-2")::contains)).isEqualTo("ca-chua-3");
        assertThat(Slugs.unique("ca-chua", Set.<String>of()::contains)).isEqualTo("ca-chua");
    }
}
