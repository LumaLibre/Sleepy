INSERT INTO migrations (source_key, imported_rows, migrated_at)
VALUES (?, ?, ?)
ON CONFLICT(source_key) DO UPDATE SET
    imported_rows = excluded.imported_rows,
    migrated_at = excluded.migrated_at;
