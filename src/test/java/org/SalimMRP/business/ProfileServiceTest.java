package org.SalimMRP.business;

import org.SalimMRP.business.dto.LeaderboardEntry;
import org.SalimMRP.business.dto.MediaDetails;
import org.SalimMRP.business.dto.MediaSearchCriteria;
import org.SalimMRP.business.dto.UserProfile;
import org.SalimMRP.persistence.FavoriteRepository;
import org.SalimMRP.persistence.RatingRepository;
import org.SalimMRP.persistence.UserRepository;
import org.SalimMRP.persistence.models.Media;
import org.SalimMRP.persistence.models.Rating;
import org.SalimMRP.persistence.models.RatingSummary;
import org.SalimMRP.persistence.models.User;
import org.SalimMRP.persistence.models.UserRatingCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceTest {

    private StubUserRepository userRepository;
    private StubRatingRepository ratingRepository;
    private StubFavoriteRepository favoriteRepository;
    private StubMediaService mediaService;
    private ProfileService profileService;

    @BeforeEach
    void setup() {
        userRepository = new StubUserRepository();
        ratingRepository = new StubRatingRepository();
        favoriteRepository = new StubFavoriteRepository();
        mediaService = new StubMediaService();
        profileService = new DefaultProfileService(userRepository, ratingRepository, favoriteRepository, mediaService);
    }

    @Test
    void buildProfileAggregatesStatsAndFavoriteGenre() {
        User user = new User("alice", "hash");
        userRepository.save(user);
        Media sciFi = mediaWithGenres(1, "Sci-Fi");
        Media drama = mediaWithGenres(2, "Drama");
        mediaService.mediaById.put(1, sciFi);
        mediaService.mediaById.put(2, drama);
        favoriteRepository.mark(user.getId(), 1);
        favoriteRepository.mark(user.getId(), 2);
        ratingRepository.addRating(rating(user.getId(), 1, 5));
        ratingRepository.addRating(rating(user.getId(), 2, 3));

        UserProfile profile = profileService.buildProfile(user.getId());

        assertNotNull(profile);
        assertEquals(2, profile.getTotalRatings());
        assertEquals(4.0, profile.getAverageRating(), 0.0001);
        assertEquals("Sci-Fi", profile.getFavoriteGenre());
        assertEquals(2, profile.getFavoritesCount());
    }

    @Test
    void ratingHistoryReturnsExactEntries() {
        User user = new User("bob", "pw");
        userRepository.save(user);
        ratingRepository.addRating(rating(user.getId(), 5, 4));

        List<Rating> history = profileService.ratingHistory(user.getId());

        assertEquals(1, history.size());
        assertEquals(5, history.get(0).getMediaId());
    }

    @Test
    void leaderboardRespectsRequestedLimit() {
        ratingRepository.userCounts.put(1, 10L);
        ratingRepository.userCounts.put(2, 5L);
        userRepository.save(new User("top", "pw"));
        userRepository.save(new User("mid", "pw"));

        List<LeaderboardEntry> entries = profileService.leaderboard(1);

        assertEquals(1, entries.size());
        assertEquals("top", entries.get(0).getUsername());
    }

    @Test
    void favoriteMediaDelegatesToMediaService() {
        Media sample = mediaWithGenres(9, "Adventure");
        mediaService.favoriteMedia.add(MediaDetails.of(sample, 4.0, 2, 1, true, List.of()));

        List<MediaDetails> favorites = profileService.favoriteMedia(42);

        assertEquals(1, favorites.size());
        assertEquals(9, favorites.get(0).getMedia().getId());
    }

    private Rating rating(int userId, int mediaId, int stars) {
        Rating rating = new Rating();
        rating.setUserId(userId);
        rating.setMediaId(mediaId);
        rating.setStarValue(stars);
        rating.setComment("c");
        rating.setCommentConfirmed(true);
        rating.setCreatedAt(Instant.now());
        return rating;
    }

    private Media mediaWithGenres(int id, String genre) {
        Media media = new Media();
        media.setId(id);
        media.setTitle("media" + id);
        media.setDescription("desc");
        media.setMediaType("Movie");
        media.setReleaseYear(2019);
        media.setAgeRestriction("PG");
        media.setGenres(List.of(genre));
        media.setCreatedByUserId(1);
        return media;
    }

    // --- Stubs ---

    private static class StubUserRepository implements UserRepository {
        private final Map<Integer, User> byId = new HashMap<>();
        private final Map<String, User> byName = new HashMap<>();
        private int nextId = 1;

        @Override
        public boolean save(User user) {
            user.setId(nextId++);
            byId.put(user.getId(), new User(user.getId(), user.getUsername(), user.getPassword()));
            byName.put(user.getUsername(), user);
            return true;
        }

        @Override
        public User findByUsername(String username) {
            User found = byName.get(username);
            return found == null ? null : new User(found.getId(), found.getUsername(), found.getPassword());
        }

        @Override
        public User findById(int id) {
            User found = byId.get(id);
            return found == null ? null : new User(found.getId(), found.getUsername(), found.getPassword());
        }
    }

    private static class StubRatingRepository implements RatingRepository {
        private final List<Rating> ratings = new ArrayList<>();
        private int nextId = 1;
        private final Map<Integer, Rating> byId = new HashMap<>();
        private final Map<Integer, Rating> byMediaUser = new HashMap<>();
        private final Map<Integer, Set<Integer>> likes = new HashMap<>();
        private final Map<Integer, Long> userCounts = new LinkedHashMap<>();

        void addRating(Rating rating) {
            save(rating);
        }

        @Override
        public Rating save(Rating rating) {
            Rating stored = clone(rating);
            stored.setId(nextId++);
            ratings.add(stored);
            byId.put(stored.getId(), stored);
            byMediaUser.put(hashKey(stored.getMediaId(), stored.getUserId()), stored);
            userCounts.merge(stored.getUserId(), 1L, Long::sum);
            return clone(stored);
        }

        @Override
        public boolean update(Rating rating) {
            return true;
        }

        @Override
        public boolean delete(int id) {
            return byId.remove(id) != null;
        }

        @Override
        public Rating findById(int id) {
            return clone(byId.get(id));
        }

        @Override
        public Rating findByMediaIdAndUserId(int mediaId, int userId) {
            return clone(byMediaUser.get(hashKey(mediaId, userId)));
        }

        @Override
        public List<Rating> findByMediaId(int mediaId) {
            return ratings.stream()
                    .filter(r -> r.getMediaId() == mediaId)
                    .map(this::clone)
                    .toList();
        }

        @Override
        public List<Rating> findByUserId(int userId) {
            return ratings.stream()
                    .filter(r -> r.getUserId() == userId)
                    .map(this::clone)
                    .toList();
        }

        @Override
        public List<RatingSummary> summarizeByMediaIds(List<Integer> mediaIds) {
            return List.of();
        }

        @Override
        public List<UserRatingCount> findRatingCountsPerUser(int limit) {
            return userCounts.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(limit)
                    .map(entry -> new UserRatingCount(entry.getKey(), entry.getValue()))
                    .toList();
        }

        @Override
        public boolean confirmComment(int ratingId) {
            return false;
        }

        @Override
        public boolean addLike(int ratingId, int userId) {
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

        private int hashKey(int mediaId, int userId) {
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

    private static class StubFavoriteRepository implements FavoriteRepository {
        private final Map<Integer, Set<Integer>> favorites = new HashMap<>();

        void mark(int userId, int mediaId) {
            addFavorite(userId, mediaId);
        }

        @Override
        public boolean addFavorite(int userId, int mediaId) {
            return favorites.computeIfAbsent(userId, key -> new HashSet<>()).add(mediaId);
        }

        @Override
        public boolean removeFavorite(int userId, int mediaId) {
            Set<Integer> perUser = favorites.get(userId);
            if (perUser == null) {
                return false;
            }
            return perUser.remove(mediaId);
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
            for (Set<Integer> perUser : favorites.values()) {
                if (perUser.contains(mediaId)) {
                    count++;
                }
            }
            return count;
        }
    }

    private static class StubMediaService implements MediaService {
        private final Map<Integer, Media> mediaById = new HashMap<>();
        private final List<MediaDetails> favoriteMedia = new ArrayList<>();

        @Override
        public boolean createMedia(Media media) {
            return false;
        }

        @Override
        public List<Media> getAllMedia() {
            return List.of();
        }

        @Override
        public Media getMediaById(int id) {
            return mediaById.get(id);
        }

        @Override
        public boolean updateMedia(Media media) {
            return false;
        }

        @Override
        public boolean deleteMedia(int id) {
            return false;
        }

        @Override
        public List<MediaDetails> searchMedia(MediaSearchCriteria criteria, int requestingUserId) {
            return List.of();
        }

        @Override
        public MediaDetails getDetailedMedia(int id, int requestingUserId) {
            return null;
        }

        @Override
        public boolean addFavorite(int mediaId, int userId) {
            return false;
        }

        @Override
        public boolean removeFavorite(int mediaId, int userId) {
            return false;
        }

        @Override
        public List<MediaDetails> listFavorites(int userId) {
            return favoriteMedia;
        }

        @Override
        public List<MediaDetails> recommendMedia(int userId) {
            return List.of();
        }
    }
}
