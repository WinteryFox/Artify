CREATE SCHEMA media;
CREATE SCHEMA interactions;
CREATE SCHEMA tokens;

/*CREATE TABLE users
(
    id            UUID        NOT NULL PRIMARY KEY,
    email         TEXT        NOT NULL UNIQUE,
    username      VARCHAR(16) NOT NULL,
    discriminator VARCHAR(5)  NOT NULL,
    display_name  TEXT        NOT NULL,
    avatar        TEXT,
    UNIQUE (username, discriminator)
);*/

CREATE TABLE users
(
    id       UUID NOT NULL PRIMARY KEY,
    email    TEXT NOT NULL UNIQUE,
    handle   TEXT NOT NULL UNIQUE,
    username TEXT NOT NULL,
    avatar   TEXT
);

CREATE TABLE tokens.email
(
    email  TEXT                        NOT NULL PRIMARY KEY,
    token  TEXT                        NOT NULL,
    expiry TIMESTAMP WITHOUT TIME ZONE NOT NULL
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
    id               BIGINT  NOT NULL PRIMARY KEY,
    user_id          UUID    NOT NULL REFERENCES users (id),
    title            TEXT    NOT NULL CHECK (length(title) > 0 AND length(title) <= 100),
    body             TEXT    NOT NULL CHECK (length(body) <= 5000),
    comments_enabled BOOLEAN NOT NULL,
    is_private       BOOLEAN NOT NULL,
    is_ai            BOOLEAN NOT NULL,
    hashes           TEXT[]  NOT NULL CHECK (array_length(hashes, 1) <= 10)
);

CREATE TABLE interactions.likes
(
    user_id UUID   NOT NULL REFERENCES users (id),
    post_id BIGINT NOT NULL REFERENCES media.illustrations (id),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE interactions.follows
(
    target_id UUID NOT NULL REFERENCES users (id),
    user_id   UUID NOT NULL REFERENCES users (id),
    PRIMARY KEY (target_id, user_id),
    CHECK (target_id != user_id)
);
