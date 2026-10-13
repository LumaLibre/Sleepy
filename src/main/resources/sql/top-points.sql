SELECT uuid, last_name, playtime_seconds, afk_seconds, points
FROM player_playtime
WHERE points > 0
ORDER BY points DESC, last_name ASC
LIMIT ?;
