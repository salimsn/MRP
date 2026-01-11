package org.SalimMRP.persistence.models;

// Kapselt Aggregationswerte zu Ratings eines Media-Eintrags.
public class RatingSummary {
    private final int mediaId;
    private final double averageScore;
    private final int ratingCount;

    public RatingSummary(int mediaId, double averageScore, int ratingCount) {
        this.mediaId = mediaId;
        this.averageScore = averageScore;
        this.ratingCount = ratingCount;
    }

    public int getMediaId() {
        return mediaId;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public int getRatingCount() {
        return ratingCount;
    }
}
