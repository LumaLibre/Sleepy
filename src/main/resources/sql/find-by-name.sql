SELECT uuid, last_name, playtime_seconds, afk_seconds, points
FROM player_playtime
WHERE last_name = ? COLLATE NOCASE;
