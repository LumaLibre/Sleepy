INSERT INTO player_playtime (
    uuid,
    last_name,
    playtime_seconds,
    afk_seconds,
    updated_at,
    points
) VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT(uuid) DO UPDATE SET
    last_name = excluded.last_name,
    playtime_seconds = MAX(player_playtime.playtime_seconds, excluded.playtime_seconds),
    afk_seconds = MAX(player_playtime.afk_seconds, excluded.afk_seconds),
    updated_at = excluded.updated_at;
