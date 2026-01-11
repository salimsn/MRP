package org.SalimMRP.business;

import org.SalimMRP.persistence.MediaRepository;
import org.SalimMRP.persistence.RatingRepository;
import org.SalimMRP.persistence.models.Media;
import org.SalimMRP.persistence.models.Rating;
import org.SalimMRP.persistence.models.RatingSummary;
import org.SalimMRP.persistence.models.UserRatingCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RatingServiceTest {

    private StubRatingRepository ratingRepository;
    private StubMediaRepository mediaRepository;
    private RatingService ratingService;

    @BeforeEach
    void setup() {
        ratingRepository = new StubRatingRepository();
        mediaRepository = new StubMediaRepository();
        ratingService = new DefaultRatingService(ratingRepository, mediaRepository);
    }

    @Test
    void createRatingPersistsWhenMediaExists() {
        mediaRepository.store(sampleMedia(1));
        Rating rating = sampleRating(0, 1, 10, 5);

        Rating saved = ratingService.createRating(rating);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
    }

    @Test
    void createRatingRejectsDuplicatePerUser() {
        mediaRepository.store(sampleMedia(1));
        ratingService.createRating(sampleRating(0, 1, 10, 4));

        assertNull(ratingService.createRating(sampleRating(0, 1, 10, 5)));
    }

    @Test
    void updateRatingChangesStarsAndResetsConfirmation() {
        mediaRepository.store(sampleMedia(1));
        Rating original = ratingService.createRating(sampleRating(0, 1, 10, 3));
        original.setCommentConfirmed(true);
        ratingRepository.update(original);

        Rating update = sampleRating(original.getId(), 1, 10, 5);
        update.setComment("edited");
        assertTrue(ratingService.updateRating(update, 10));

        Rating stored = ratingRepository.findById(original.getId());
        assertEquals(5, stored.getStarValue());
        assertFalse(stored.isCommentConfirmed());
    }

    @Test
    void deleteRatingRefusesForeignEntries() {
        mediaRepository.store(sampleMedia(1));
        Rating rating = ratingService.createRating(sampleRating(0, 1, 10, 4));

        assertFalse(ratingService.deleteRating(rating.getId(), 99));
        assertTrue(ratingService.deleteRating(rating.getId(), 10));
    }

    @Test
    void confirmCommentRequiresText() {
        mediaRepository.store(sampleMedia(1));
        Rating rating = ratingService.createRating(sampleRating(0, 1, 10, 4));
        rating.setComment("");
        ratingRepository.update(rating);

        assertFalse(ratingService.confirmComment(rating.getId(), 10));
    }

    @Test
    void likeAndUnlikeFlow() {
        mediaRepository.store(sampleMedia(1));
        Rating rating = ratingService.createRating(sampleRating(0, 1, 10, 4));
        Rating otherUserRating = ratingService.createRating(sampleRating(0, 1, 11, 5));

        assertTrue(ratingService.likeRating(otherUserRating.getId(), 10));
        assertFalse(ratingService.likeRating(otherUserRating.getId(), 11), "cannot like own rating");
        assertTrue(ratingService.unlikeRating(otherUserRating.getId(), 10));
    }

    private Rating sampleRating(int id, int mediaId, int userId, int stars) {
        Rating rating = new Rating();
        rating.setId(id);
        rating.setMediaId(mediaId);
        rating.setUserId(userId);
        rating.setStarValue(stars);
        rating.setComment("comment");
        rating.setCommentConfirmed(false);
        rating.setCreatedAt(Instant.now());
        return rating;
    }

    private Media sampleMedia(int id) {
        Media media = new Media();
        media.setId(id);
        media.setTitle("Title" + id);
        media.setDescription("desc");
        media.setMediaType("Movie");
        media.setReleaseYear(2020);
        media.setAgeRestriction("PG");
        media.setGenres(List.of("Adventure"));
        media.setCreatedByUserId(1);
        return media;
    }

    // --- stubs ---

    private static class StubMediaRepository implements MediaRepository {
        private final Map<Integer, Media> storage = new HashMap<>();

        void store(Media media) {
            storage.put(media.getId(), media);
        }

        @Override
        public boolean save(Media media) {
            storage.put(media.getId(), media);
            return true;
        }

        @Override
        public List<Media> findAll() {
            return List.copyOf(storage.values());
        }

        @Override
        public Media findById(int id) {
            return storage.get(id);
        }

        @Override
        public boolean update(Media media) {
            storage.put(media.getId(), media);
            return true;
        }

        @Override
        public boolean delete(int id) {
            return storage.remove(id) != null;
        }
    }

    private static class StubRatingRepository implements RatingRepository {
        private final Map<Integer, Rating> storage = new HashMap<>();
        private final Map<Integer, Rating> byMediaUser = new HashMap<>();
        private final Map<Integer, Set<Integer>> likes = new HashMap<>();
        private int nextId = 1;

        @Override
        public Rating save(Rating rating) {
            Rating copy = clone(rating);
            copy.setId(nextId++);
            storage.put(copy.getId(), copy);
            byMediaUser.put(key(copy.getMediaId(), copy.getUserId()), copy);
            return clone(copy);
        }

        @Override
        public boolean update(Rating rating) {
            storage.put(rating.getId(), clone(rating));
            byMediaUser.put(key(rating.getMediaId(), rating.getUserId()), rating);
            return true;
        }

        @Override
        public boolean delete(int id) {
            Rating removed = storage.remove(id);
            if (removed != null) {
                byMediaUser.remove(key(removed.getMediaId(), removed.getUserId()));
            }
            return removed != null;
        }

        @Override
        public Rating findById(int id) {
            return clone(storage.get(id));
        }

        @Override
        public Rating findByMediaIdAndUserId(int mediaId, int userId) {
            return clone(byMediaUser.get(key(mediaId, userId)));
        }

        @Override
        public List<Rating> findByMediaId(int mediaId) {
            List<Rating> list = new ArrayList<>();
            for (Rating rating : storage.values()) {
                if (rating.getMediaId() == mediaId) {
                    list.add(clone(rating));
                }
            }
            return list;
        }

        @Override
        public List<Rating> findByUserId(int userId) {
            List<Rating> list = new ArrayList<>();
            for (Rating rating : storage.values()) {
                if (rating.getUserId() == userId) {
                    list.add(clone(rating));
                }
            }
            return list;
        }

        @Override
        public List<RatingSummary> summarizeByMediaIds(List<Integer> mediaIds) {
            return List.of();
        }

        @Override
        public List<UserRatingCount> findRatingCountsPerUser(int limit) {
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
            if (rating == null || rating.getUserId() == userId) {
                return false;
            }
            return likes.computeIfAbsent(ratingId, key -> new HashSet<>()).add(userId);
        }

        @Override
        public boolean removeLike(int ratingId, int userId) {
            return likes.getOrDefault(ratingId, Set.of()).contains(userId)
                    && likes.get(ratingId).remove(userId);
        }

        @Override
        public Set<Integer> findLikes(int ratingId) {
            return likes.getOrDefault(ratingId, Set.of());
        }

        private int key(int mediaId, int userId) {
            return Objects.hash(mediaId, userId);
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
}
