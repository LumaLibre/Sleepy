package dev.lumas.sleepy.storage

enum class SqlStatement(private val resourceName: String) {
    SCHEMA("schema.sql"),
    PLAYER_COLUMNS("player-columns.sql"),
    ADD_POINTS_COLUMN("add-points-column.sql"),
    RENAME_POINTS_COLUMN("rename-points-column.sql"),
    LOAD_PLAYER("load-player.sql"),
    SAVE_PLAYER("save-player.sql"),
    SAVE_POINTS("save-points.sql"),
    UPDATE_NAME("update-name.sql"),
    FIND_BY_NAME("find-by-name.sql"),
    FIND_BY_UUID("find-by-uuid.sql"),
    TOP_PLAYTIME("top-playtime.sql"),
    TOP_AFK_TIME("top-afk-time.sql"),
    MIGRATION_EXISTS("migration-exists.sql"),
    RECORD_MIGRATION("record-migration.sql"),
    JETS_SELECT_PLAYTIME("jets-select-playtime.sql"),
    ;

    val text: String by lazy {
        checkNotNull(javaClass.getResourceAsStream("/sql/$resourceName")) {
            "Missing SQL resource: $resourceName"
        }.bufferedReader(Charsets.UTF_8).use { it.readText().trim() }
    }
}
