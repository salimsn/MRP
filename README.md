# Media Ratings Platform (MRP)

Repository: https://github.com/salimsn/MRP

## Überblick
MRP ist eine REST-API zum Verwalten von Medien (Filme, Serien, Spiele) und deren Bewertungen. Registrierte Nutzer können Medien anlegen, bearbeiten, bewerten, Kommentare moderieren, Likes vergeben, Favoriten pflegen, Empfehlungen abrufen sowie Profil- und Leaderboard-Auswertungen einsehen. Die Anwendung läuft als schlanker Java-Server ohne schweres Webframework.

## Technologie-Stack
- Java 24, Maven
- Eingebauter `com.sun.net.httpserver`
- PostgreSQL (Schema in `src/main/resources/db/init.sql`)
- JDBC-Repositories und schlanke Service-Schicht
- Jackson (inkl. `jackson-datatype-jsr310` für Java-Time)

## Voraussetzungen
- JDK 24 und Maven 3.9+
- PostgreSQL mit den Standardzugängen `postgres:postgres` auf Port 5433
- Optional Docker und Docker Compose

## Anwendung starten
1. Datenbank hochfahren  
   ```bash
   docker compose up -d postgres
   ```
2. Projekt bauen  
   ```bash
   mvn clean compile
   ```
3. Server starten  
   ```bash
   mvn exec:java -Dexec.mainClass=org.SalimMRP.application.Main -Dexec.classpathScope=runtime
   ```
4. API steht unter http://localhost:8080 bereit. Beenden mit `CTRL+C`.

## API in Kurzform
- **Authentifizierung**: `POST /api/users/register`, `POST /api/users/login` (liefert Bearer-Token).
- **Profil & Nutzerfunktionen** (Token nötig):
  - `GET /api/users/{username}/profile`
  - `GET /api/users/{username}/ratings`
  - `GET /api/users/{username}/favorites`
  - `GET /api/users/leaderboard?limit=10`
- **Medienverwaltung** (Token nötig):
  - `GET /api/media?title=&genre=&mediaType=&releaseYear=&ageRestriction=&minRating=&sort=&direction=`
  - `POST /api/media`
  - `GET /api/media/{id}`
  - `PUT /api/media/{id}`
  - `DELETE /api/media/{id}`
  - `POST /api/media/{id}/favorites` / `DELETE /api/media/{id}/favorites`
  - `GET /api/media/recommendations`
- **Bewertungen** (Token nötig):
  - `GET /api/ratings/media/{mediaId}`
  - `POST /api/ratings/media/{mediaId}`
  - `PUT /api/ratings/{ratingId}`
  - `DELETE /api/ratings/{ratingId}`
  - `POST /api/ratings/{ratingId}/confirm`
  - `POST /api/ratings/{ratingId}/likes` / `DELETE /api/ratings/{ratingId}/likes`

## Datenbank & Authentifizierung
- Tabellen: `users`, `media` (inkl. `release_year`, `age_restriction`, `genres`), `ratings`, `rating_likes`, `favorites`.
- Schema liegt in `src/main/resources/db/init.sql` und wird beim Docker-Start automatisch eingespielt.
- Passwörter werden via SHA-256 gehasht; Tokens liegen im Speicher (`InMemoryTokenService`).
- Für produktive Szenarien sollten Token-Ablauf, persistente Token und stärkere Passwort-Hashing-Algorithmen ergänzt werden.

## Tests
Es existieren mehr als zwanzig Unit-Tests für Benutzer-, Medien-, Profil- und Sicherheitslogik (`src/test/java`). Ausgeführt wird mit:

```bash
mvn test
```
