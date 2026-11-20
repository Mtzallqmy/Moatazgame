package com.masalik.yemen.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        GarageVehicleEntity::class,
        MissionRecordEntity::class,
        SettingsHudEntity::class,
        RadioPreferenceEntity::class,
        DrivingTripLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MasalikDatabase : RoomDatabase() {
    abstract fun masalikDao(): MasalikDao

    companion object {
        const val DB_NAME = "masalik_room_db"

        @Volatile
        private var INSTANCE: MasalikDatabase? = null

        fun getInstance(context: Context): MasalikDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MasalikDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
