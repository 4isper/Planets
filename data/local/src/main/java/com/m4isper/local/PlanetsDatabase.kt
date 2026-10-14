package com.m4isper.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.m4isper.local.dao.ApodDao
import com.m4isper.local.dao.RemoteKeysDao
import com.m4isper.local.entity.ApodEntity
import com.m4isper.local.entity.RemoteKeys

@Database(
    entities = [ApodEntity::class, RemoteKeys::class],
    version = PlanetsDatabase.VERSION,
    exportSchema = true,
)
abstract class PlanetsDatabase : RoomDatabase() {

    abstract fun apodDao(): ApodDao

    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        /**
         * 1, because nothing has shipped. Bumping this requires a `Migration`:
         * `schemas/` is committed so the next one has a baseline to diff against.
         */
        const val VERSION = 1
    }
}