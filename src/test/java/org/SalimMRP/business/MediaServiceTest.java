package org.SalimMRP.business;

import org.SalimMRP.business.dto.MediaDetails;
import org.SalimMRP.business.dto.MediaSearchCriteria;
import org.SalimMRP.persistence.FavoriteRepository;
import org.SalimMRP.persistence.MediaRepository;
import org.SalimMRP.persistence.RatingRepository;
import org.SalimMRP.persistence.models.Media;
import org.SalimMRP.persistence.models.Rating;
import org.SalimMRP.persistence.models.RatingSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MediaServiceTest {

    private StubMediaRepository mediaRepository;
    private StubRatingRepository ratingRepository;
    private StubFavoriteRepository favoriteRepository;
    private MediaService mediaService;

    @BeforeEach
    void setup() {
        mediaRepository = new StubMediaRepository();
        ratingRepository = new StubRatingRepository();
        favoriteRepository = new StubFavoriteRepository();
        mediaService = new DefaultMediaService(mediaRepository, ratingRepository, favoriteRepository);
    }

    @Test
    void createMediaRejectsMissingMandatoryFields() {
        Media invalid = sampleMedia("Broken", "Movie", 1);
        invalid.setReleaseYear(null);

        assertFalse(mediaService.createMedia(invalid));
        assertTrue(mediaRepository.storage.isEmpty());
    }

    @Test
    void createMediaPersistsValidEntity() {
        Media media = sampleMedia("Matrix", "Movie", 2);

        assertTrue(mediaService.createMedia(media));
        assertEquals(1, mediaRepository.storage.size());
        assertTrue(media.getId() > 0);
    }

    @Test
    void searchMediaRespectsTitleGenreAndRating() {
        Media star = sampleMedia("Star Saga", "Movie", 1);
        star.setGenres(List.of("Sci-Fi"));
        Media other = sampleMedia("Drama", "Movie", 1);
        other.setGenres(List.of("Drama"));
        mediaRepository.save(star);
        mediaRepository.save(other);
        ratingRepository.save(rating(star.getId(), 10, 5));
        ratingRepository.save(rating(other.getId(), 11, 2));

        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setTitleQuery("star");
        criteria.setGenre("SCI-FI");
        criteria.setMinimumRating(4.0);

        List<MediaDetails> result = mediaService.searchMedia(criteria, 99);
        assertEquals(1, result.size());
        assertEquals(star.getId(), result.get(0).getMedia().getId());
    }

    @Test
    void updateMediaFailsWhenUnknown() {
        Media detached = sampleMedia("Unknown", "Movie", 5);
        detached.setId(999);

        assertFalse(mediaService.updateMedia(detached));
    }

    @Test
    void favoriteCountsIncreaseAndListReflectsState() {
        Media title = sampleMedia("Adventure", "Movie", 3);
        mediaRepository.save(title);

        assertTrue(mediaService.addFavorite(title.getId(), 7));
        assertEquals(1, mediaService.listFavorites(7).size());
        assertEquals(1, favoriteRepository.countFavoritesForMedia(title.getId()));
        assertTrue(mediaService.removeFavorite(title.getId(), 7));
        assertEquals(0, mediaService.listFavorites(7).size());
    }

    @Test
    void recommendationFallsBackToPopularWhenNoHistory() {
        Media first = sampleMedia("Popular One", "Game", 1);
        Media second = sampleMedia("Popular Two", "Game", 1);
        mediaRepository.save(first);
        mediaRepository.save(second);
        ratingRepository.save(rating(first.getId(), 1, 5));
        ratingRepository.save(rating(second.getId(), 2, 4));

        List<MediaDetails> recommendations = mediaService.recommendMedia(5);

        assertEquals(2, recommendations.size());
        assertEquals("Popular One", recommendations.get(0).getMedia().getTitle());
    }

    private Media sampleMedia(String title, String type, int creator) {
        Media media = new Media();
        media.setTitle(title);
        media.setDescription("Desc " + title);
        media.setMediaType(type);
        media.setReleaseYear(2020);
        media.setAgeRestriction("PG-13");
        media.setGenres(List.of("Adventure"));
        media.setCreatedByUserId(creator);
        return media;
    }

    private Rating rating(int mediaId, int userId, int stars) {
        Rating rating = new Rating();
        rating.setMediaId(mediaId);
        rating.setUserId(userId);
        rating.setStarValue(stars);
        rating.setComment("c" + stars);
        rating.setCommentConfirmed(true);
        rating.setCreatedAt(Instant.now());
        return rating;
    }

    // --- Test doubles ---

    private static class StubMediaRepository implements MediaRepository {
        private final Map<Integer, Media> storage = new HashMap<>();
        private int nextId = 1;

        @Override
        public boolean save(Media media) {
            if (media == null) {
                return false;
            }
            Media copy = clone(media);
            copy.setId(nextId++);
            storage.put(copy.getId(), copy);
            media.setId(copy.getId());
            return true;
        }

        @Override
        public List<Media> findAll() {
            return storage.values().stream().map(this::clone).toList();
        }

        @Override
        public Media findById(int id) {
            return clone(storage.get(id));
        }

        @Override
        public boolean update(Media media) {
            if (media == null || !storage.containsKey(media.getId())) {
                return false;
            }
            storage.put(media.getId(), clone(media));
            return true;
        }

        @Override
        public boolean delete(int id) {
            return storage.remove(id) != null;
        }

        private Media clone(Media media) {
            if (media == null) {
                return null;
            }
            Media clone = new Media();
            clone.setId(media.getId());
            clone.setTitle(media.getTitle());
            clone.setDescription(media.getDescription());
            clone.setMediaType(media.getMediaType());
            clone.setReleaseYear(media.getReleaseYear());
            clone.setAgeRestriction(media.getAgeRestriction());
            clone.setGenres(media.getGenres());
            clone.setCreatedByUserId(media.getCreatedByUserId());
            return clone;
        }
    }

    private static class StubRatingRepository implements RatingRepository {
        private final Map<Integer, Rating> storage = new HashMap<>();
        private int nextId = 1;

        @Override
        public Rating save(Rating rating) {
            Rating copy = clone(rating);
            copy.setId(nextId++);
            storage.put(copy.getId(), copy);
            rating.setId(copy.getId());
            return clone(copy);
        }

        @Override
        public boolean update(Rating rating) {
            storage.put(rating.getId(), clone(rating));
            return true;
        }

        @Override
        public boolean delete(int id) {
            return storage.remove(id) != null;
        }

        @Override
        public Rating findById(int id) {
            return clone(storage.get(id));
        }

        @Override
        public Rating findByMediaIdAndUserId(int mediaId, int userId) {
            return storage.values().stream()
                    .filter(r -> r.getMediaId() == mediaId && r.getUserId() == userId)
                    .findFirst()
                    .map(this::clone)
                    .orElse(null);
        }

        @Override
        public List<Rating> findByMediaId(int mediaId) {
            return storage.values().stream()
                    .filter(r -> r.getMediaId() == mediaId)
                    .map(this::clone)
                    .toList();
        }

        @Override
        public List<Rating> findByUserId(int userId) {
            return storage.values().stream()
                    .filter(r -> r.getUserId() == userId)
                    .map(this::clone)
                    .toList();
        }

        @Override
        public List<RatingSummary> summarizeByMediaIds(List<Integer> mediaIds) {
            List<RatingSummary> summaries = new ArrayList<>();
            for (Integer id : mediaIds) {
                List<Rating> ratings = findByMediaId(id);
                if (ratings.isEmpty()) {
                    continue;
                }
                double avg = ratings.stream().mapToInt(Rating::getStarValue).average().orElse(0.0);
                summaries.add(new RatingSummary(id, avg, ratings.size()));
            }
            return summaries;
        }

        @Override
        public List<org.SalimMRP.persistence.models.UserRatingCount> findRatingCountsPerUser(int limit) {
            return List.of();
        }

        @Override
        public boolean confirmComment(int ratingId) {
            Rating rating = storage.get(ratingId);
            if (rating == null) {
                return false;
            }
            rating.setCommentConfirmed(true);
            return true;
        }

        @Override
        public boolean addLike(int ratingId, int userId) {
            Rating rating = storage.get(ratingId);
            return rating != null && rating.likeByUser(userId);
        }

        @Override
        public boolean removeLike(int ratingId, int userId) {
            Rating rating = storage.get(ratingId);
            return rating != null && rating.unlikeByUser(userId);
        }

        @Override
        public Set<Integer> findLikes(int ratingId) {
            Rating rating = storage.get(ratingId);
            return rating == null ? Set.of() : rating.getLikedByUserIds();
        }

        private Rating clone(Rating rating) {
            if (rating == null) {
                return null;
            }
            Rating copy = new Rating();
            copy.setId(rating.getId());
            copy.setMediaId(rating.getMediaId());
            copy.setUserId(rating.getUserId());
            copy.setStarValue(rating.getStarValue());
            copy.setComment(rating.getComment());
            copy.setCommentConfirmed(rating.isCommentConfirmed());
            copy.setCreatedAt(rating.getCreatedAt());
            copy.setLikedByUserIds(new HashSet<>(rating.getLikedByUserIds()));
            return copy;
        }
    }

    private static class StubFavoriteRepository implements FavoriteRepository {
        private final Map<Integer, Set<Integer>> favorites = new HashMap<>();

        @Override
        public boolean addFavorite(int userId, int mediaId) {
            return favorites.computeIfAbsent(userId, key -> new HashSet<>()).add(mediaId);
        }

        @Override
        public boolean removeFavorite(int userId, int mediaId) {
            return favorites.getOrDefault(userId, Set.of()).contains(mediaId)
                    && favorites.get(userId).remove(mediaId);
        }

        @Override
        public boolean isFavorite(int userId, int mediaId) {
            return favorites.getOrDefault(userId, Set.of()).contains(mediaId);
        }

        @Override
        public List<Integer> findMediaIdsByUser(int userId) {
            return new ArrayList<>(favorites.getOrDefault(userId, Set.of()));
        }

        @Override
        public int countFavoritesForMedia(int mediaId) {
            int count = 0;
            for (Set<Integer> userFavorites : favorites.values()) {
                if (userFavorites.contains(mediaId)) {
                    count++;
                }
            }
            return count;
        }
    }
}
