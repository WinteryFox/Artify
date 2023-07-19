CREATE TABLE users
(
    id       UUID NOT NULL PRIMARY KEY,
    handle   TEXT NOT NULL UNIQUE,
    username TEXT NOT NULL,
    avatar   TEXT
);

CREATE SCHEMA metadata;

CREATE TABLE metadata.tags
(
    id SMALLSERIAL PRIMARY KEY
);

CREATE SCHEMA illustrations;

CREATE TABLE illustrations.posts
(
    id               BIGINT  NOT NULL PRIMARY KEY,
    user_id          UUID    NOT NULL REFERENCES users (id),
    title            TEXT    NOT NULL CHECK (length(title) > 0 AND length(title) <= 100),
    body             TEXT    NOT NULL CHECK (length(body) <= 5000),
    comments_enabled BOOLEAN NOT NULL,
    hashes           TEXT[]  NOT NULL CHECK (array_length(hashes, 1) > 0 AND array_length(hashes, 1) <= 10)
);

CREATE TABLE illustrations.tags
(
    tag_id  INT    NOT NULL REFERENCES metadata.tags (id),
    post_id BIGINT NOT NULL REFERENCES illustrations.posts (id),
    PRIMARY KEY (tag_id, post_id)
);

CREATE SCHEMA interactions;

CREATE TABLE interactions.likes
(
    user_id UUID   NOT NULL REFERENCES users (id),
    post_id BIGINT NOT NULL REFERENCES illustrations.posts (id),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE interactions.follows
(
    target_id UUID NOT NULL REFERENCES users (id),
    user_id   UUID NOT NULL REFERENCES users (id),
    CHECK (target_id != user_id),
    PRIMARY KEY (target_id, user_id)
);
