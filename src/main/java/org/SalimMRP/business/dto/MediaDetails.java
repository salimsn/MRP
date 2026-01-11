package org.SalimMRP.business.dto;

import org.SalimMRP.persistence.models.Media;
import org.SalimMRP.persistence.models.Rating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Enthält alle Informationen, die für die Darstellung eines Media-Eintrags benötigt werden.
public class MediaDetails {
    private final Media media;
    private final double averageRating;
    private final int ratingCount;
    private final int favoritesCount;
    private final boolean favoriteForUser;
    private final List<Rating> ratings;

    private MediaDetails(Media media,
                         double averageRating,
                         int ratingCount,
                         int favoritesCount,
                         boolean favoriteForUser,
                         List<Rating> ratings) {
        this.media = media;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.favoritesCount = favoritesCount;
        this.favoriteForUser = favoriteForUser;
        this.ratings = Collections.unmodifiableList(new ArrayList<>(ratings));
    }

    public static MediaDetails of(Media media,
                                  double averageRating,
                                  int ratingCount,
                                  int favoritesCount,
                                  boolean favoriteForUser,
                                  List<Rating> ratings) {
        return new MediaDetails(media, averageRating, ratingCount, favoritesCount, favoriteForUser, ratings);
    }

    public Media getMedia() {
        return media;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public boolean isFavoriteForUser() {
        return favoriteForUser;
    }

    public List<Rating> getRatings() {
        return ratings;
    }
}
