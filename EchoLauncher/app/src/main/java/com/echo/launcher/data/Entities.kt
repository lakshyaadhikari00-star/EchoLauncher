package com.echo.launcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per voice/typed command the user has issued. This is the local history log. */
@Entity(tableName = "command_log")
data class CommandEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long
)

/** Apps the user has pinned to the top row, in the order they chose. */
@Entity(tableName = "pinned_apps")
data class PinnedApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val sortOrder: Int
)
