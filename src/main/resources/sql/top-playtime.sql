SELECT uuid, last_name, playtime_seconds, afk_seconds, points
FROM player_playtime
ORDER BY playtime_seconds DESC, last_name ASC
LIMIT ?;
