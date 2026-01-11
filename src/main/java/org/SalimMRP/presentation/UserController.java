package org.SalimMRP.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.SalimMRP.business.MediaService;
import org.SalimMRP.business.ProfileService;
import org.SalimMRP.business.UserService;
import org.SalimMRP.persistence.models.User;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// Registriert die Benutzer-Endpunkte und stellt Hilfsfunktionen für Antworten bereit.
public class UserController {

    private final ObjectMapper mapper;
    private final UserService userService;
    private final ProfileService profileService;
    private final MediaService mediaService;

    // Service und JSON-Mapper werden über den Konstruktor injiziert.
    public UserController(UserService userService,
                          ProfileService profileService,
                          MediaService mediaService,
                          ObjectMapper mapper) {
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.profileService = Objects.requireNonNull(profileService, "profileService must not be null");
        this.mediaService = Objects.requireNonNull(mediaService, "mediaService must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public UserService getUserService() {
        return userService;
    }

    public ProfileService getProfileService() {
        return profileService;
    }

    public MediaService getMediaService() {
        return mediaService;
    }

    public void registerRoutes(HttpServer server) {
        server.createContext("/api/users/register", new RegisterHandler(this));
        server.createContext("/api/users/login", new LoginHandler(this));
        server.createContext("/api/users", new UserHandler(this));
    }

    // Sendet eine Text-Antwort mit dem gewünschten Statuscode.
    public void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    // Serialisiert ein Objekt nach JSON und setzt den passenden Content-Type.
    public void sendJsonResponse(HttpExchange exchange, int statusCode, Object response) throws IOException {
        String json = mapper.writeValueAsString(response);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    // Prüft das Authorization-Header und gibt den zugehörigen Benutzer zurück.
    public User authenticate(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "Missing or invalid Authorization header");
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        if (!userService.isTokenValid(token)) {
            sendResponse(exchange, 401, "Invalid or expired token");
            return null;
        }

        User user = userService.getUserByToken(token);
        if (user == null) {
            sendResponse(exchange, 401, "Unknown user for token");
        }
        return user;
    }
}
