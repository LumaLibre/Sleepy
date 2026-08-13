CREATE TABLE IF NOT EXISTS player_playtime (
    uuid TEXT PRIMARY KEY,
    last_name TEXT NOT NULL COLLATE NOCASE,
    playtime_seconds INTEGER NOT NULL DEFAULT 0,
    afk_seconds INTEGER NOT NULL DEFAULT 0,
    points INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_player_playtime_rank
    ON player_playtime(playtime_seconds DESC);

CREATE INDEX IF NOT EXISTS idx_player_afk_rank
    ON player_playtime(afk_seconds DESC);

CREATE TABLE IF NOT EXISTS migrations (
    source_key TEXT PRIMARY KEY,
    imported_rows INTEGER NOT NULL,
    migrated_at INTEGER NOT NULL
);
