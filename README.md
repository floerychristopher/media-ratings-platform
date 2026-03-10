# Media Ratings Platform (MRP)

Eine eigenständige RESTful HTTP-Server-Anwendung, entwickelt in Java (ohne Frameworks wie Spring).
Sie stellt eine API bereit, mit der Benutzer Medieninhalte (Filme, Serien, Spiele) verwalten, bewerten und zu Favoriten hinzufügen können.

---

## Verwendete Tools

* **Java 21 (JDK)**
* **Maven** (für Dependency-Management und Build)
* **Docker & Docker Compose** (für die PostgreSQL-Datenbank)
* **Git** (Versionskontrolle)
* **curl** oder **Postman** (für Integrationstests)

---

# Setup und Start der Anwendung

## 1. Datenbank starten

Die Anwendung nutzt eine PostgreSQL-Datenbank.
Die `docker-compose.yml` befindet sich im Root-Verzeichnis des Projekts.

Öffne den Terminal und führe folgenden Befehl aus:

```bash
docker compose up -d
```

Dies startet eine PostgreSQL-Instanz mit folgenden Zugangsdaten:

* **Host:** `localhost`
* **Port:** `5432`
* **User:** `mrp`
* **Password:** `mrp`
* **Database:** `mrp`

---

## 2. Projekt bauen

Verwende Maven, um die Abhängigkeiten herunterzuladen und das Projekt zu kompilieren:

```bash
mvn clean install
```

---

## 3. Server starten

Starte die `Main`-Klasse im folgenden Verzeichnis:

```
src/main/java/mrp/Main.java
```

Der selbst implementierte HTTP-Server startet anschließend und lauscht auf:

```
http://localhost:9090
```

**Hinweis:**
Das Datenbankschema (Tabellen) wird beim Start der Anwendung automatisch durch den DatabaseManager erstellt.

---

# Tests

## Unit Tests (Business Logic)

Das Projekt enthält **21 Unit-Tests**, die die Kernlogik der Service-Schicht testen.

Verwendete Tools:

* **JUnit 5**
* **Mockito**

Um die Unit Tests auszuführen:

```bash
mvn test
```

---

## Integration Tests

Ein umfassendes **Bash-Testskript (`integration-test.sh`)** testet den kompletten API-Workflow:

* Registrierung
* Login
* CRUD-Operationen
* Bewertungen (Ratings)
* Favoriten
* Leaderboard
* Suche & Filter

### Voraussetzungen

* Der Java-Server muss laufen
* Die Docker-Datenbank muss gestartet sein

### Tests ausführen

```bash
./integration-integration-test.sh
```

---

# Repository

GitHub Repository:
https://github.com/floerychristopher/media-ratings-platform
