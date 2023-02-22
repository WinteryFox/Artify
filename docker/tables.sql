CREATE SCHEMA media;

CREATE TABLE media.assets
(
    id   TEXT PRIMARY KEY,
    hash TEXT
);

CREATE TABLE users
(
    id TEXT PRIMARY KEY,
    avatar TEXT REFERENCES media.assets (id)
);

CREATE TABLE media.posts
(
    id          TEXT PRIMARY KEY,
    author      TEXT REFERENCES users (id),
    title       TEXT,
    description TEXT,
    attachments TEXT REFERENCES media.assets (id)
);

CREATE SCHEMA interactions;

CREATE TABLE interactions.likes
(
    user_id TEXT REFERENCES users (id),
    post_id TEXT REFERENCES media.posts (id)
);
