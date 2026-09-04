package com.arka.vpn.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {

    // OnConflictStrategy.IGNORE + ایندکس یکتای hash → لینک‌های تکراری insert نمی‌شن.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(configs: List<ConfigEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM configs WHERE category = :category")
    suspend fun countByCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM configs")
    suspend fun countAll(): Int

    @Query("SELECT * FROM configs WHERE category = :category ORDER BY addedAt DESC")
    suspend fun getByCategory(category: String): List<ConfigEntity>

    @Query("SELECT * FROM configs WHERE category = :category ORDER BY addedAt DESC")
    fun observeByCategory(category: String): Flow<List<ConfigEntity>>

    @Query("DELETE FROM configs")
    suspend fun deleteAll()
}
