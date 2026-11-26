package com.recipehub.community;

/** Điểm trung bình (làm tròn 1 chữ số) và số đánh giá hiển thị của một công thức. */
public record RatingStats(Long recipeId, Double avg, long count) {

    public RatingStats {
        if (avg != null) avg = Math.round(avg * 10) / 10.0;
    }
}
