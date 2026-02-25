CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE media (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    media_type VARCHAR(50) NOT NULL,  -- movie, series or game
    release_year INT,
    genre VARCHAR(255),
    age_restriction INT,
    created_by INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE ratings (
    id SERIAL PRIMARY KEY,
    media_id INT REFERENCES media(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id),
    stars INT CHECK (stars BETWEEN 1 AND 5),
    comment TEXT,
    comment_visible BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(media_id, user_id)
);

CREATE TABLE rating_likes (
    rating_id INT REFERENCES ratings(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id),
    PRIMARY KEY (rating_id, user_id)
);

CREATE TABLE favorites (
    user_id INT REFERENCES users(id),
    media_id INT REFERENCES media(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, media_id)
);