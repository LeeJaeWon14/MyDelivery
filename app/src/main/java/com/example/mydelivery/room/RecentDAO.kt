package com.example.mydelivery.room

import androidx.room.*

@Dao
interface RecentDAO {
    @Query("SELECT DISTINCT * FROM RecentEntity")
    fun selectRecent() : List<RecentEntity>

    @Insert
    fun insertRecent(entity: RecentEntity)

    @Update
    fun updateRecent(entity: RecentEntity)

    @Delete
    fun deleteRecent(entity: RecentEntity)

    @Query("DELETE FROM RecentEntity")
    fun removeAll()
}