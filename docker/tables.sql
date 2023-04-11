CREATE SCHEMA media;
CREATE SCHEMA interactions;

CREATE TABLE media.assets
(
    hash      TEXT NOT NULL PRIMARY KEY,
    mime_type TEXT NOT NULL
);

CREATE TABLE users
(
    id       TEXT NOT NULL PRIMARY KEY,
    username TEXT NOT NULL,
    avatar   TEXT REFERENCES media.assets (hash)
);

CREATE TABLE media.tags
(
    id       BIGINT NOT NULL,
    language TEXT   NOT NULL,
    src      TEXT   NOT NULL,
    PRIMARY KEY (id, language)
);

CREATE TABLE media.illustrations
(
    id                   BIGINT  NOT NULL PRIMARY KEY,
    user_id              TEXT    NOT NULL REFERENCES users (id),
    title                TEXT    NOT NULL,
    body                 TEXT    NOT NULL,
    has_comments_enabled BOOLEAN NOT NULL,
    is_private           BOOLEAN NOT NULL,
    is_ai                BOOLEAN NOT NULL
);

CREATE TABLE media.attachments
(
    post_id    BIGINT NOT NULL REFERENCES media.illustrations (id),
    asset_hash TEXT   NOT NULL REFERENCES media.assets (hash),
    PRIMARY KEY (post_id, asset_hash)
);

CREATE TABLE interactions.likes
(
    user_id TEXT   NOT NULL REFERENCES users (id),
    post_id BIGINT NOT NULL REFERENCES media.illustrations (id),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE interactions.follows
(
    follower_id TEXT NOT NULL REFERENCES users (id),
    followee_id TEXT NOT NULL REFERENCES users (id),
    CHECK (follower_id != followee_id),
    PRIMARY KEY (follower_id, followee_id)
);
