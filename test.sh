#!/bin/bash

BASE_URL="http://localhost:9090/api"

echo "=========================================="
echo "   MRP API INTEGRATION TESTS"
echo "=========================================="
echo ""

# --- 1. AUTHENTIFIZIERUNG ---
echo "=== 1. User 1 registrieren & einloggen ==="
curl -s -X POST "$BASE_URL/users/register" -H "Content-Type: application/json" -d '{"Username":"user1", "Password":"pwd"}' > /dev/null
curl -s -X POST "$BASE_URL/users/login" -H "Content-Type: application/json" -d '{"Username":"user1", "Password":"pwd"}' > /dev/null
TOKEN1="user1-mrpToken"
echo "User 1 bereit."

echo "=== 2. User 2 registrieren & einloggen ==="
curl -s -X POST "$BASE_URL/users/register" -H "Content-Type: application/json" -d '{"Username":"user2", "Password":"pwd"}' > /dev/null
curl -s -X POST "$BASE_URL/users/login" -H "Content-Type: application/json" -d '{"Username":"user2", "Password":"pwd"}' > /dev/null
TOKEN2="user2-mrpToken"
echo "User 2 bereit."
echo -e "\n"

# --- 2. USER PROFILE ---
echo "=== 3. Profil von User 1 aktualisieren ==="
# Erwartet: 200 OK
curl -i -X PUT "$BASE_URL/users/user1/profile" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN1" \
     -d '{"Bio": "Ich liebe Filme!"}'
echo -e "\n\n"

# --- 3. MEDIA CRUD ---
echo "=== 4. User 1 erstellt einen Film (Sollte ID 1 bekommen) ==="
# Erwartet: 201 Created
curl -i -X POST "$BASE_URL/media" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN1" \
     -d '{
           "title": "The Matrix",
           "description": "Willkommen in der echten Welt.",
           "mediaType": "movie",
           "releaseYear": 1999,
           "genres": ["sci-fi", "action"],
           "ageRestriction": 16
         }'
echo -e "\n\n"

echo "=== 5. User 1 aktualisiert den Film (ID 1) ==="
# Erwartet: 200 OK
curl -i -X PUT "$BASE_URL/media/1" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN1" \
     -d '{
           "title": "The Matrix - Reloaded",
           "description": "Willkommen in der echten Welt.",
           "mediaType": "movie",
           "releaseYear": 2003,
           "genres": ["sci-fi", "action"],
           "ageRestriction": 16
         }'
echo -e "\n\n"

# --- 4. RATINGS & LIKES & MODERATION ---
echo "=== 6. User 2 bewertet den Film (Media ID 1 -> Rating ID 1) ==="
# Erwartet: 201 Created
curl -i -X POST "$BASE_URL/media/1/rate" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN2" \
     -d '{
           "stars": 4,
           "comment": "Gute Action!"
         }'
echo -e "\n\n"

echo "=== 7. User 2 aktualisiert seine Bewertung (Rating ID 1) ==="
# Erwartet: 200 OK
curl -i -X PUT "$BASE_URL/ratings/1" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN2" \
     -d '{
           "stars": 5,
           "comment": "Meisterwerk!"
         }'
echo -e "\n\n"

echo "=== 8. User 1 liked die Bewertung von User 2 (Rating ID 1) ==="
# Erwartet: 200 OK
curl -i -X POST "$BASE_URL/ratings/1/like" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

echo "=== 9. User 1 (Ersteller) bestätigt den Kommentar (Rating ID 1) ==="
# Erwartet: 200 OK
curl -i -X POST "$BASE_URL/ratings/1/confirm" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

# --- 5. FAVORITEN ---
echo "=== Favorit hinzufügen: User 2 fügt Media 1 zu Favoriten hinzu ==="
# Erwartet: 200 OK
curl -i -X POST "$BASE_URL/media/1/favorite" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

echo "=== Favoriten abrufen: User 2 holt seine Favoritenliste ==="
# Erwartet: 200 OK (Sollte eine Liste mit dem Film zurückgeben)
curl -i -X GET "$BASE_URL/users/user2/favorites" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

echo "=== Favorit entfernen: User 2 entfernt Media 1 wieder ==="
# Erwartet: 200 OK
curl -i -X DELETE "$BASE_URL/media/1/favorite" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

# --- 6. SEARCH & FILTER ---
echo "=== Vorbereitung Suche: User 1 erstellt einen 2. Film ==="
curl -s -X POST "$BASE_URL/media" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN1" \
     -d '{
           "title": "Interstellar",
           "description": "Ins schwarze Loch.",
           "mediaType": "movie",
           "releaseYear": 2014,
           "genres": ["sci-fi", "drama"],
           "ageRestriction": 12
         }' > /dev/null

echo "=== Suche 1: Alle Sci-Fi Filme, sortiert nach Erscheinungsjahr ==="
# Erwartet: Beide Filme in der Liste, Interstellar (2014) vor Matrix (2003)
curl -i -X GET "$BASE_URL/media?genre=sci-fi&sortBy=year" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

echo "=== Suche 2: Partial Matching nach Titel ('inter') ==="
# Erwartet: Nur Interstellar
curl -i -X GET "$BASE_URL/media?title=inter" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

echo "=== Suche 3: Filter nach Score (>= 4 Sterne) ==="
# Erwartet: Nur The Matrix (hat von User 2 5 Sterne bekommen)
curl -i -X GET "$BASE_URL/media?rating=4&sortBy=score" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

# --- 7. LEADERBOARD ---
echo "=== Leaderboard abrufen ==="
# Erwartet: 200 OK. User 2 sollte vor User 1 sein (da User 2 das Rating erstellt hat)
curl -i -X GET "$BASE_URL/leaderboard"
echo -e "\n\n"

# --- 8. PROFIL STATS & RATING HISTORY ---
echo "=== Rating History abrufen: User 2 holt seine Bewertungen ==="
# Erwartet: 200 OK mit einer Liste seiner Ratings
curl -i -X GET "$BASE_URL/users/user2/ratings" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

echo "=== Profil abrufen: User 2 schaut seine eigenen Stats an ==="
# Erwartet: 200 OK. Das JSON sollte jetzt ratingCount: 1, averageScore: 5.0 und favoriteGenre: "sci-fi" enthalten
curl -i -X GET "$BASE_URL/users/user2/profile" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

# --- 9. CLEANUP ---
echo "=== 10. User 1 löscht den Film (Media ID 1) ==="
# Erwartet: 204 No Content
curl -i -X DELETE "$BASE_URL/media/1" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n"

echo "=========================================="
echo "   TESTLAUF BEENDET"
echo "=========================================="