package com.lockit.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dateKey = :dateKey ORDER BY id ASC")
    fun getTasksForDate(dateKey: String): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE recurrence = 'RECURRING' AND duration = 'LONG_TERM'")
    suspend fun getRecurringTemplates(): List<Task>

    @Query("SELECT COALESCE(SUM(weightagePercent), 0) FROM tasks WHERE dateKey = :dateKey")
    suspend fun getTotalWeightageForDate(dateKey: String): Int

    @Query("SELECT COALESCE(SUM(weightagePercent), 0) FROM tasks WHERE dateKey = :dateKey AND isCompleted = 1")
    fun getCompletedWeightageForDate(dateKey: String): Flow<Int>
}
