package com.m4isper.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.m4isper.local.dao.ApodDao
import com.m4isper.local.dao.RemoteKeysDao
import com.m4isper.local.entity.ApodEntity
import com.m4isper.local.entity.RemoteKeys

@Database(
    entities = [ApodEntity::class, RemoteKeys::class],
    version = 5,
    exportSchema = false,
)
abstract class PlanetsDatabase : RoomDatabase() {

    abstract fun apodDao(): ApodDao

    abstract fun remoteKeysDao(): RemoteKeysDao
}