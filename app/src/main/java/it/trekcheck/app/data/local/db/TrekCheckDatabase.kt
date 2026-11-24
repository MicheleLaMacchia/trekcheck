package it.trekcheck.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RouteMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TrekCheckDatabase : RoomDatabase() {
    abstract fun routeMetadataDao(): RouteMetadataDao

    companion object {
        const val DATABASE_NAME = "trekcheck.db"

        fun create(context: Context): TrekCheckDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TrekCheckDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
