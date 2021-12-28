package com.example.mydelivery.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecentEntity::class], version = 2, exportSchema = true)
abstract class MyRoomDatabase : RoomDatabase() {
    abstract fun getRecentDAO() : RecentDAO

    companion object {
        private var instance: MyRoomDatabase? = null
        fun getInstance(context: Context) : MyRoomDatabase {
            instance?.let {
                return it
            } ?: run {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyRoomDatabase::class.java,
                    "recentRoom.db"
                ).fallbackToDestructiveMigration()
                    .build()
                return instance!!
            }
        }
    }
}