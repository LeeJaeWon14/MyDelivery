package com.example.mydelivery.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class RecentEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "company")
    var company: String,
    @ColumnInfo(name = "track_number")
    var trackNumber: String,
    @ColumnInfo(name = "company_name")
    var companyName: String
) : Serializable