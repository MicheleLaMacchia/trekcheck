package it.trekcheck.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteMetadataDao {
    @Query("SELECT * FROM route_metadata ORDER BY updatedAtUtcEpochMillis DESC, id ASC")
    fun observeAll(): Flow<List<RouteMetadataEntity>>

    @Query("SELECT * FROM route_metadata WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RouteMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RouteMetadataEntity)

    @Query("DELETE FROM route_metadata WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM route_metadata")
    suspend fun count(): Int
}
