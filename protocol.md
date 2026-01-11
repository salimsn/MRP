# Protokoll – Media Ratings Platform

Repository: https://github.com/if24b257/MRP

## App-Design und Struktur
- **Zielsetzung:** REST-API für Medienverwaltung, Bewertungen, Favoriten, Profile und Empfehlungen ohne volles Framework. Alles soll leicht nachvollziehbar und lokal lauffähig bleiben.
- **Schichten:**  
  ```
  Main -> Controllers/Handlers -> Business-Services -> Repositories -> PostgreSQL
  ```
  *Application* startet den `HttpServer`. *Presentation* sind die Handler (`UserHandler`, `MediaHandler`, `RatingHandler`). *Business* bündelt Logik (`DefaultUserService`, `DefaultMediaService`, …). *Persistence* ist Plain-JDBC (`JdbcUserRepository` usw.).  
- **Wichtige Entscheidungen:** Eingebauter `HttpServer`, Jackson für JSON, SHA-256 für Passwörter, Tokens im Speicher (`InMemoryTokenService`). PostgreSQL läuft per Docker-Compose; Schema liegt in `src/main/resources/db/init.sql`.  
- **Klassenskizze (verkürzt):**
  ```
  UserController
    ├─ RegisterHandler
    ├─ LoginHandler
    └─ UserHandler
  MediaController
    └─ MediaHandler
  RatingController
    └─ RatingHandler

  DefaultUserService -> UserRepository, PasswordHasher, TokenService
  DefaultMediaService -> MediaRepository, RatingRepository, FavoriteRepository
  DefaultRatingService -> RatingRepository, MediaRepository
  DefaultProfileService -> UserRepository, RatingRepository, FavoriteRepository, MediaService
  ```

## Lessons Learned
1. **Saubere Trennung spart Zeit:** Während der Entwicklung war es hilfreich, dass Controller keine DB-Details kannten. Änderungen an den JDBC-Repos mussten so nie in den HTTP-Handlern angepasst werden.
2. **Frühe Postman-Collection:** Das manuelle Durchklicken half, Auth-Flow und Favoriten schnell zu überprüfen. Die Collection wurde später auch zur Dokumentation.
3. **Datenbank-Reset einplanen:** Nach mehreren Testläufen brauchte ich einen unkomplizierten Reset (Truncate/Volume-Löschen). Das steht jetzt im README und spart Sucherei.

## Unit-Testing-Strategie
- **Umfang:** 24 Tests (User-, Media-, Rating-, Profile-Services + Sicherheitskomponenten). Jeder Test nutzt kleine In-Memory- oder Stub-Repositories, damit kein echter Datenbankzugriff nötig ist.
- **Abdeckung:**  
  - `UserServiceTest` prüft Registrierung (Validierung, Hashing), Login und Token-Auflösung.  
  - `MediaServiceTest` fokussiert auf Validierung, Suche/Filter, Favoriten und Empfehlungsfallbacks.  
  - `RatingServiceTest` deckt Einmalbewertung pro Nutzer, Updates, Kommentarbestätigungen und Like/Unlike ab.  
  - `ProfileServiceTest` kontrolliert aggregierte Profilzahlen, Historie und Leaderboard.  
  - `SecurityComponentsTest` verifiziert Hashing und Token-Verhalten.  
- **Philosophie:** Business-Logik möglichst isoliert testen, damit Änderungen an der DB-Schicht keine Unit-Tests brechen. Integrationstests laufen manuell mit Postman.

## SOLID-Beispiele
1. **Single Responsibility:** `UserController` kümmert sich nur um HTTP-Aspekte (Header prüfen, JSON schreiben). Die eigentliche Logik (Registrierung, Login) steckt in `DefaultUserService`. Dadurch bleiben Controller schlank und austauschbar.
2. **Dependency Inversion:** Jeder Service bekommt Interfaces injiziert (z. B. `MediaService` -> `MediaRepository`, `RatingRepository`). In den Tests lassen sich dadurch triviale Stub-Implementierungen nutzen, ohne Produktionscode zu verändern.
