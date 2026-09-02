package com.lockit.app.util

/**
 * Implements the weightage -> unlock-time rule described by the user:
 *   - Each task has a weightage 0-100%.
 *   - Completing tasks whose weightage sums to X% unlocks X% of the app's
 *     daily allotted usage time.
 *   - Completing tasks that sum to 100% (i.e. ALL tasks for the day) removes
 *     the lock entirely and grants the full daily usage time.
 *
 * Example from the spec: 3 tasks A, B, C each weighted 10%.
 *   - A + B done -> 20% weightage done -> 20% of allotted time unlocked.
 *   - C done too (100% of *defined* weightage, not literally 100 since only
 *     30% of the scale was assigned) -> ALL locks removed, full time granted.
 *
 * To support that exact example (where the 3 tasks only add up to 30%, not
 * 100%), "full unlock" triggers once the user has completed every task
 * created for that day - not only when the sum hits literal 100.
 */
object UnlockCalculator {

    data class UnlockResult(
        val allottedMinutes: Int,
        val unlockedPercent: Int,
        val unlockedMinutes: Int,
        val isFullyUnlocked: Boolean
    )

    fun calculate(
        dailyAllottedMinutes: Int,
        totalTasksToday: Int,
        completedTasksToday: Int,
        completedWeightageSum: Int
    ): UnlockResult {
        val isFullyUnlocked = totalTasksToday > 0 && completedTasksToday == totalTasksToday

        val effectivePercent = when {
            isFullyUnlocked -> 100
            else -> completedWeightageSum.coerceIn(0, 100)
        }

        val minutes = (dailyAllottedMinutes * effectivePercent) / 100

        return UnlockResult(
            allottedMinutes = dailyAllottedMinutes,
            unlockedPercent = effectivePercent,
            unlockedMinutes = minutes,
            isFullyUnlocked = isFullyUnlocked
        )
    }

    /** Validates that weightages assigned across a day's tasks never exceed 100. */
    fun canAssignWeightage(currentTotalExcludingThisTask: Int, newWeightage: Int): Boolean {
        return currentTotalExcludingThisTask + newWeightage <= 100
    }

    /** Suggests likely ML Kit labels from a task title, e.g. "Have breakfast" -> food-related. */
    fun suggestLabelsForTitle(title: String): List<String> {
        val t = title.lowercase()
        return when {
            "breakfast" in t || "lunch" in t || "dinner" in t || "eat" in t || "meal" in t ->
                listOf("food", "plate", "dish", "meal", "table")
            "walk" in t || "run" in t || "jog" in t ->
                listOf("outdoor", "footwear", "road", "path", "sky")
            "gym" in t || "workout" in t || "exercise" in t ->
                listOf("gym", "fitness", "exercise equipment", "person")
            "water" in t || "hydrate" in t || "drink" in t ->
                listOf("bottle", "glass", "water", "cup")
            "read" in t || "book" in t ->
                listOf("book", "text", "page")
            "medicine" in t || "medication" in t || "vitamin" in t || "pill" in t ->
                listOf("pill", "medication", "bottle", "tablet")
            "clean" in t || "tidy" in t ->
                listOf("room", "furniture", "floor")
            else -> emptyList()
        }
    }
}
