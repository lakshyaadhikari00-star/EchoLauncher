package com.echo.launcher.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {
    @Insert
    suspend fun insert(entry: CommandEntry)

    @Query("SELECT * FROM command_log ORDER BY timestamp DESC LIMIT 20")
    fun recent(): Flow<List<CommandEntry>>

    @Query("DELETE FROM command_log")
    suspend fun clear()
}

@Dao
interface PinnedAppDao {
    @Insert
    suspend fun insert(app: PinnedApp)

    @Delete
    suspend fun delete(app: PinnedApp)

    @Query("SELECT * FROM pinned_apps ORDER BY sortOrder ASC")
    fun all(): Flow<List<PinnedApp>>

    @Query("SELECT COUNT(*) FROM pinned_apps WHERE packageName = :pkg")
    suspend fun isPinned(pkg: String): Int
}
