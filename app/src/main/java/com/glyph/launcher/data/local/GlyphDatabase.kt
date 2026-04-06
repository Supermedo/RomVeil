package com.glyph.launcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.glyph.launcher.data.local.entity.GameEntity

@Database(
    entities = [GameEntity::class],
    version = 3,
    exportSchema = false
)
abstract class GlyphDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
