INSERT INTO player_playtime (
    uuid,
    last_name,
    playtime_seconds,
    afk_seconds,
    points,
    updated_at
) VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT(uuid) DO UPDATE SET
    last_name = excluded.last_name,
    points = excluded.points,
    updated_at = excluded.updated_at;
