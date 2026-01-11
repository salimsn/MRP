package org.SalimMRP.persistence.models;

// Aggregiertes Ergebnis für Leaderboards: Anzahl der Ratings pro Benutzer.
public class UserRatingCount {
    private final int userId;
    private final long ratingCount;

    public UserRatingCount(int userId, long ratingCount) {
        this.userId = userId;
        this.ratingCount = ratingCount;
    }

    public int getUserId() {
        return userId;
    }

    public long getRatingCount() {
        return ratingCount;
    }
}
