# Protokoll – Media Ratings Platform

## 1. App-Design und Architektur
Die Anwendung wurde von Grund auf ohne externe Web-Frameworks entwickelt.  
Sie implementiert einen eigenen HTTP-Server (`HttpServer` und `HttpRequest`/`HttpResponse`) sowie einen dynamischen `Router`, der HTTP-Methoden und Pfadvariablen (z. B. `/api/media/{id}`) parst.

Für eine saubere, wartbare und skalierbare Codebasis wurde eine **3-Schichten-Architektur** umgesetzt:

1. **Controller-Schicht:** Dient als Einstiegspunkt. Sie parst eingehende HTTP-Anfragen, extrahiert JSON-Payloads und Pfadparameter, behandelt Token-basierte Authentifizierung über den `TokenManager`, delegiert Aufgaben an die Services und gibt passende HTTP-Statuscodes zurück (z. B. 201 Created, 404 Not Found).

2. **Service-Schicht:** Enthält die reine Kern-Business-Logik. Validiert Eingaben (z. B. ob Sternbewertungen zwischen 1–5 liegen), setzt Sicherheitsregeln durch (z. B. nur der Ersteller kann einen Kommentar bestätigen) und verwaltet Transaktionen konzeptionell.

3. **Repository-Schicht:** Interagiert ausschließlich mit der PostgreSQL-Datenbank. Verwendet `PreparedStatement`, um SQL-Injection zu verhindern, und mappt `ResultSet`-Zeilen zurück auf Java-Domänenmodelle (`User`, `Media`, `Rating`).

Die Abhängigkeiten werden zentral in der `Main.java` verdrahtet (Dependency Injection).

---

## 2. Unit-Testing-Strategie und Coverage
Die Tests konzentrieren sich darauf, die Kern-Business-Logik in der **Service-Schicht** (`UserService`, `MediaService`, `RatingService`) isoliert zu testen.

* **Frameworks:** JUnit 5 für die Testausführung und Mockito für das Mocking von Abhängigkeiten.
* **Warum Mocking?** Um die Business-Logik unabhängig von der Datenbank zu testen. Durch das Mocken der Repositories laufen die Tests schnell und sind nicht von Datenbankzuständen oder Verbindungsproblemen abhängig. Außerdem können Edge-Cases simuliert werden (z. B. ein erzwungenes `SQLException`, um Unique-Constraint-Verletzungen zu testen).
* **Coverage:** Es wurden 21 aussagekräftige Unit-Tests implementiert. Sie decken sowohl die „Happy Paths“ (erfolgreiche Erstellung/Updates) als auch erwartete Exceptions ab (z. B. `IllegalArgumentException` für ungültige Eingaben, `SecurityException` für unautorisierte Aktionen, `IllegalStateException` für doppelte Einträge).

---

## 3. Implementierte SOLID-Prinzipien

### Single Responsibility Principle (SRP)
Jede Klasse im Projekt hat genau einen Grund zur Änderung.  
*Beispiel:* `MediaController` ist ausschließlich für die HTTP-Übersetzung zuständig. Er enthält weder SQL-Abfragen noch Business-Logik. Ändert sich die HTTP-API (z. B. ein Routenname), wird nur der Controller angepasst. Ändert sich eine Geschäftsregel (z. B. Mindestjahr für Veröffentlichungen), wird nur der `MediaService` geändert.

### Dependency Inversion Principle (DIP) / Dependency Injection
High-Level-Module (Services) hängen nicht direkt von Low-Level-Modulen (Repositories) ab, indem sie diese selbst instanziieren. Stattdessen werden Abhängigkeiten über Konstruktoren injiziert.  
*Beispiel:* In `Main.java` wird der `DatabaseManager` instanziiert und an das `UserRepository` übergeben. Anschließend wird das `UserRepository` an den `UserService` übergeben. So kann während der Unit-Tests leicht ein Mock-Repository verwendet werden, strikt nach DIP.

---

## 4. Lessons Learned & Problembehandlung
* **Custom HTTP Parsing:** Das Parsen von rohen HTTP-Byte-Streams in Objekte war herausfordernd, insbesondere das Trennen von Headern und Body und das exakte Lesen des `Content-Length`. Die Implementierung eines Routers, der dynamisch Pfadvariablen (`{id}`) extrahiert, erforderte ein gutes Verständnis von String-Manipulation und Arrays.
* **SQL-Injection-Vermeidung:** Dynamische Suchabfragen (Filtern nach Genre, Titel usw.) verlockten zunächst zu einfacher String-Konkatenation. Zur Vermeidung von SQL-Injection wurde gelernt, dynamische Strings mit `?`-Platzhaltern zu bauen und eine dynamische Liste von Objekten zu verwenden, die sicher an ein `PreparedStatement` gebunden werden.
* **PostgreSQL-Striktheit:** Bei Berechnungen von Benutzerstatistiken warf PostgreSQL Fehler bezüglich `GROUP BY` und unnesteter Arrays. Die Nutzung von `CROSS JOIN LATERAL unnest(...) AS t(column_name)` erwies sich als saubere, alias-sichere Methode, um komma-separierte Strings für Aggregationen zu expandieren.

---

## 5. Zeitplanung (geschätzt)

| Aufgabe | Geschätzte Stunden |
| :--- |:-------------------|
| Projekt-Setup & Custom HTTP Server / Router | 8.0 h              |
| Datenbankschema & DB Manager Setup | 3.0 h              |
| Benutzer-Authentifizierung (TokenManager, BCrypt, Register/Login) | 4.5 h              |
| Media CRUD-Operationen & Controller | 5.0 h              |
| Bewertungssystem (Sterne, Kommentare, Moderation, Likes) | 5.5 h              |
| Favoriten & Profilstatistiken (komplexe SQL) | 4.0 h              |
| Suche & Filter (dynamische SQL-Implementierung) | 4.0 h              |
| Leaderboard & Refactoring | 2.5 h              |
| Unit-Tests (JUnit 5 + Mockito) | 5.0 h              |
| Dokumentation & Integrationstest-Skript (`test.sh`) | 2.5 h              |
| **Gesamtschätzung** | **45.0 h**         |