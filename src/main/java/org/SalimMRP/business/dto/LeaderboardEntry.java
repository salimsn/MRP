package org.SalimMRP.business.dto;

// Ein einzelner Eintrag der öffentlichen Bestenliste.
public class LeaderboardEntry {
    private final String username;
    private final long ratingCount;

    public LeaderboardEntry(String username, long ratingCount) {
        this.username = username;
        this.ratingCount = ratingCount;
    }

    public String getUsername() {
        return username;
    }

    public long getRatingCount() {
        return ratingCount;
    }
}
