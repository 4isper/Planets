package com.m4isper.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.m4isper.local.entity.RemoteKeys
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteKeysDao {

    @Query("SELECT * FROM remote_keys WHERE id = 0")
    fun observeKeys(): Flow<RemoteKeys?>

    @Query("SELECT * FROM remote_keys WHERE id = 0")
    suspend fun getKeys(): RemoteKeys?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keys: RemoteKeys)

    @Query("DELETE FROM remote_keys")
    suspend fun clear()
}