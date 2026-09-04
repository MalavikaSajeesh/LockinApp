package com.lockin.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dateKey = :dateKey ORDER BY sortOrder ASC, id ASC")
    fun getTasksForDate(dateKey: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dateKey = :dateKey ORDER BY sortOrder ASC, id ASC")
    suspend fun getTasksForDateOnce(dateKey: String): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks")
    suspend fun getAllOnce(): List<Task>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun updateAll(tasks: List<Task>)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tasks WHERE recurrence = 'RECURRING' AND dateKey = :dateKey")
    suspend fun getRecurringForDate(dateKey: String): List<Task>

    /**
     * Incomplete ONE_TIME tasks from a given day — used by the carryforward
     * logic. RECURRING tasks are excluded because they are already rolled
     * forward separately in rollForwardRecurring().
     */
    @Query(
        "SELECT * FROM tasks WHERE dateKey = :dateKey " +
        "AND isCompleted = 0 AND recurrence = 'ONE_TIME' " +
        "ORDER BY sortOrder ASC, id ASC"
    )
    suspend fun getIncompleteOneTimeForDate(dateKey: String): List<Task>

    /**
     * How many ONE_TIME tasks in [dateKey] were already carried from [fromDateKey].
     * Used to guard against running the carryforward twice on the same day.
     */
    @Query(
        "SELECT COUNT(*) FROM tasks WHERE dateKey = :dateKey " +
        "AND templateOfDateKey = :fromDateKey AND recurrence = 'ONE_TIME'"
    )
    suspend fun countNonRecurringCarriedFrom(dateKey: String, fromDateKey: String): Int

    @Query("SELECT MAX(dateKey) FROM tasks WHERE recurrence = 'RECURRING'")
    suspend fun latestRecurringDateKey(): String?

    @Query("SELECT COUNT(*) FROM tasks WHERE dateKey = :dateKey")
    suspend fun countForDate(dateKey: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tasks WHERE dateKey = :dateKey")
    suspend fun maxSortOrderForDate(dateKey: String): Int

    @Query("SELECT COALESCE(SUM(weightagePercent), 0) FROM tasks WHERE dateKey = :dateKey")
    suspend fun getTotalWeightageForDate(dateKey: String): Int

    @Query(
        "SELECT COALESCE(SUM(weightagePercent), 0) FROM tasks " +
        "WHERE dateKey = :dateKey AND id != :excludeId"
    )
    suspend fun getTotalWeightageExcluding(dateKey: String, excludeId: Long): Int

    @Query(
        """
        SELECT dateKey AS dateKey,
               COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN isCompleted THEN 1 ELSE 0 END), 0) AS completed
        FROM tasks
        GROUP BY dateKey
        ORDER BY dateKey DESC
        LIMIT 120
        """
    )
    fun observeDayStats(): Flow<List<DayStat>>
}
