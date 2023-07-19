CREATE VIEW posts_with_tags AS
SELECT *
FROM illustrations.posts AS post
         LEFT JOIN illustrations.tags AS tag ON tag.post_id = post.id;
