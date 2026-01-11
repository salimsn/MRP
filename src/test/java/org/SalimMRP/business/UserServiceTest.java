package org.SalimMRP.business;

import org.SalimMRP.business.auth.InMemoryTokenService;
import org.SalimMRP.business.auth.Sha256PasswordHasher;
import org.SalimMRP.business.auth.TokenService;
import org.SalimMRP.persistence.UserRepository;
import org.SalimMRP.persistence.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private RecordingUserRepository userRepository;
    private TokenService tokenService;
    private UserService userService;

    @BeforeEach
    void setup() {
        userRepository = new RecordingUserRepository();
        tokenService = new InMemoryTokenService();
        userService = new DefaultUserService(userRepository, new Sha256PasswordHasher(), tokenService);
    }

    @Test
    void registerStoresHashedPasswordAndAssignsId() {
        User user = new User("alice", "clear-text");

        assertTrue(userService.register(user));

        Optional<User> stored = userRepository.findRaw("alice");
        assertTrue(stored.isPresent());
        assertNotEquals("clear-text", stored.get().getPassword());
        assertTrue(stored.get().getId() > 0);
    }

    @Test
    void registerRejectsBlankCredentials() {
        assertFalse(userService.register(new User("   ", "pw")));
        assertFalse(userService.register(new User("bob", " ")));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        assertTrue(userService.register(new User("carol", "pw")));
        assertFalse(userService.register(new User("carol", "other")));
    }

    @Test
    void loginIssuesTokenForValidCredentials() {
        userService.register(new User("dave", "secret"));

        String token = userService.login("dave", "secret");

        assertNotNull(token);
        assertTrue(userService.isTokenValid(token));
    }

    @Test
    void loginFailsWithWrongPassword() {
        userService.register(new User("erin", "secret"));

        assertNull(userService.login("erin", "invalid"));
    }

    @Test
    void getUserByTokenLoadsFromRepository() {
        userService.register(new User("frank", "pw"));
        String token = userService.login("frank", "pw");

        User loaded = userService.getUserByToken(token);

        assertNotNull(loaded);
        assertEquals("frank", loaded.getUsername());
    }

    private static class RecordingUserRepository implements UserRepository {
        private final Map<Integer, User> byId = new HashMap<>();
        private final Map<String, User> byName = new HashMap<>();
        private int nextId = 1;

        @Override
        public boolean save(User user) {
            if (user == null || byName.containsKey(user.getUsername())) {
                return false;
            }
            User stored = clone(user);
            stored.setId(nextId++);
            byId.put(stored.getId(), stored);
            byName.put(stored.getUsername(), stored);
            return true;
        }

        @Override
        public User findByUsername(String username) {
            return clone(byName.get(username));
        }

        @Override
        public User findById(int id) {
            return clone(byId.get(id));
        }

        Optional<User> findRaw(String username) {
            return Optional.ofNullable(byName.get(username));
        }

        private User clone(User user) {
            if (user == null) {
                return null;
            }
            return new User(user.getId(), user.getUsername(), user.getPassword());
        }
    }
}
