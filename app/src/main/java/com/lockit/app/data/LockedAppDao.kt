package com.lockit.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps")
    fun getAll(): Flow<List<LockedApp>>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): LockedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: LockedApp)

    @Delete
    suspend fun delete(app: LockedApp)

    @Update
    suspend fun update(app: LockedApp)
}
