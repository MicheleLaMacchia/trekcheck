package it.trekcheck.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RouteMetadataEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class TrekCheckDatabase : RoomDatabase() {
    abstract fun routeMetadataDao(): RouteMetadataDao

    companion object {
        const val DATABASE_NAME = "trekcheck.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE route_metadata ADD COLUMN paceProfileType TEXT NOT NULL DEFAULT 'NORMAL'",
                )
                db.execSQL(
                    "ALTER TABLE route_metadata ADD COLUMN customFlatSpeedMps REAL DEFAULT NULL",
                )
            }
        }

        fun create(context: Context): TrekCheckDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TrekCheckDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
