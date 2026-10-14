package com.m4isper.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.m4isper.local.entity.ApodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApodDao {

    @Query("SELECT * FROM apod ORDER BY page ASC, position ASC")
    fun observeAll(): Flow<List<ApodEntity>>

    @Query("SELECT * FROM apod WHERE date = :date")
    fun observeByDate(date: String): Flow<ApodEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ApodEntity>)

    /** For a record fetched on its own, so a stale cache copy gets replaced. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ApodEntity)

    @Query("DELETE FROM apod")
    suspend fun clearAll()
}
