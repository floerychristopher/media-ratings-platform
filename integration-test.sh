#!/bin/bash

BASE_URL="http://localhost:9090/api"

echo "=========================================="
echo "   MRP API INTEGRATION TESTS (FINAL)"
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
echo "=== 3. Profil von User 1 aktualisieren (inkl. E-Mail) ==="
# Erwartet: 200 OK
curl -i -X PUT "$BASE_URL/users/user1/profile" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN1" \
     -d '{"bio": "Ich liebe Filme!", "email": "user1@example.com"}'
echo -e "\n\n"

# --- 3. MEDIA CRUD ---
echo "=== 4. User 1 erstellt einen Film (Sollte ID 1 bekommen) ==="
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

# --- 4. RATINGS & MODERATION ---
echo "=== 6. User 2 bewertet den Film (Media ID 1 -> Rating ID 1) ==="
curl -i -X POST "$BASE_URL/media/1/rate" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN2" \
     -d '{
           "stars": 4,
           "comment": "Gute Action!"
         }'
echo -e "\n\n"

echo "=== 7. User 2 aktualisiert seine Bewertung (Rating ID 1) ==="
curl -i -X PUT "$BASE_URL/ratings/1" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN2" \
     -d '{
           "stars": 5,
           "comment": "Meisterwerk!"
         }'
echo -e "\n\n"

echo "=== 8. User 1 liked die Bewertung von User 2 (Rating ID 1) ==="
curl -i -X POST "$BASE_URL/ratings/1/like" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

echo "=== 9. User 1 (Ersteller) bestätigt den Kommentar (Rating ID 1) ==="
curl -i -X POST "$BASE_URL/ratings/1/confirm" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

# --- 5. FAVORITEN ---
echo "=== 10. Favorit hinzufügen: User 2 fügt Media 1 zu Favoriten hinzu ==="
curl -i -X POST "$BASE_URL/media/1/favorite" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

echo "=== 11. Favoriten abrufen: User 2 holt seine Favoritenliste ==="
curl -i -X GET "$BASE_URL/users/user2/favorites" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

echo "=== 12. Favorit entfernen: User 2 entfernt Media 1 wieder ==="
curl -i -X DELETE "$BASE_URL/media/1/favorite" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

# --- 6. SEARCH & FILTER ---
echo "=== 13. Vorbereitung Suche: User 1 erstellt einen 2. Film ==="
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

echo "=== 14. Suche: Filter nach Score (>= 4 Sterne) ==="
# Erwartet: The Matrix - Reloaded
curl -i -X GET "$BASE_URL/media?rating=4&sortBy=score" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n\n"

# --- 7. LEADERBOARD & STATS ---
echo "=== 15. Leaderboard abrufen ==="
curl -i -X GET "$BASE_URL/leaderboard"
echo -e "\n\n"

echo "=== 16. Profil abrufen: User 2 schaut seine eigenen Stats an ==="
curl -i -X GET "$BASE_URL/users/user2/profile" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

# --- 8. CLEANUP (inkl. dem neuen Rating-Delete) ---
echo "=== 17. User 2 löscht seine eigene Bewertung (Rating ID 1) ==="
# Erwartet: 204 No Content
curl -i -X DELETE "$BASE_URL/ratings/1" \
     -H "Authorization: Bearer $TOKEN2"
echo -e "\n\n"

echo "=== 18. User 1 löscht den Film (Media ID 1) ==="
# Erwartet: 204 No Content
curl -i -X DELETE "$BASE_URL/media/1" \
     -H "Authorization: Bearer $TOKEN1"
echo -e "\n"

echo "=========================================="
echo "   TESTLAUF BEENDET"
echo "=========================================="