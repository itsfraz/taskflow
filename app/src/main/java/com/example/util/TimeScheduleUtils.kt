package com.example.util

import com.example.data.TaskEntity
import java.util.Calendar
import java.util.Locale

object TimeScheduleUtils {
    // Parse HH:mm to minutes from midnight
    fun parseToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrEmpty()) return null
        val parts = timeStr.split(":")
        if (parts.size < 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val mins = parts[1].toIntOrNull() ?: return null
        return hours * 60 + mins
    }

    // Format minutes from midnight to HH:mm (24-hour style)
    fun formatTo24H(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }

    // Format minutes from midnight to 12-hour style (e.g. 09:15 AM, 02:45 PM)
    fun formatTo12H(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        val amPm = if (h >= 12) "PM" else "AM"
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayH, m, amPm)
    }

    // Parse string of style 12-hour/24-hour back to 24H "HH:mm" for DB storage
    fun ensure24H(timeStr: String?): String? {
        if (timeStr.isNullOrEmpty()) return null
        if (timeStr.contains(":")) {
            val clean = timeStr.trim()
            val hasAm = clean.lowercase().contains("am")
            val hasPm = clean.lowercase().contains("pm")
            val rawTime = clean.replace(" AM", "", ignoreCase = true)
                               .replace(" PM", "", ignoreCase = true)
                               .replace("AM", "", ignoreCase = true)
                               .replace("PM", "", ignoreCase = true)
                               .trim()
            val parts = rawTime.split(":")
            if (parts.size >= 2) {
                var h = parts[0].toIntOrNull() ?: 12
                val m = parts[1].toIntOrNull() ?: 0
                if (hasAm || hasPm) {
                    if (hasPm && h < 12) h += 12
                    if (hasAm && h == 12) h = 0
                }
                return String.format(Locale.getDefault(), "%02d:%02d", h % 24, m % 60)
            }
        }
        return null
    }

    // Check if two tasks overlap on the same day
    fun doOverlap(t1: TaskEntity, t2: TaskEntity): Boolean {
        if (t1.dueDate == null || t2.dueDate == null || t1.dueTime == null || t2.dueTime == null) return false
        
        // Ensure same day
        val c1 = Calendar.getInstance().apply { timeInMillis = t1.dueDate }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2.dueDate }
        if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR) || c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR)) {
            return false
        }

        val s1 = parseToMinutes(t1.dueTime) ?: return false
        val s2 = parseToMinutes(t2.dueTime) ?: return false
        
        val e1 = parseToMinutes(t1.endTime) ?: (s1 + t1.estimatedDuration)
        val e2 = parseToMinutes(t2.endTime) ?: (s2 + t2.estimatedDuration)

        return s1 < e2 && s2 < e1
    }

    // Detect all conflicts (overlaps) in a set of tasks on a given day
    fun findConflicts(tasks: List<TaskEntity>): List<Pair<TaskEntity, TaskEntity>> {
        val conflicts = mutableListOf<Pair<TaskEntity, TaskEntity>>()
        val sorted = tasks.filter { it.dueTime != null && !it.isCompleted && !it.isArchived }
            .sortedBy { parseToMinutes(it.dueTime) ?: 0 }
        
        for (i in 0 until sorted.size - 1) {
            for (j in i + 1 until sorted.size) {
                if (doOverlap(sorted[i], sorted[j])) {
                    conflicts.add(Pair(sorted[i], sorted[j]))
                }
            }
        }
        return conflicts
    }

    // Suggest a free slot of certain duration in minutes on a given day
    fun findAvailableSlot(tasks: List<TaskEntity>, duration: Int = 30): Int {
        val minutesInDay = 24 * 60
        val busyIntervals = tasks.filter { it.dueTime != null && !it.isCompleted && !it.isArchived }
            .map { task ->
                val s = parseToMinutes(task.dueTime) ?: 0
                val e = parseToMinutes(task.endTime) ?: (s + task.estimatedDuration)
                Pair(s, e)
            }.sortedBy { it.first }

        var currentStart = 6 * 60 // start scanning at 6:00 AM
        for (interval in busyIntervals) {
            if (interval.first - currentStart >= duration) {
                return currentStart
            }
            if (interval.second > currentStart) {
                currentStart = interval.second
            }
        }
        if (minutesInDay - currentStart >= duration) {
            return currentStart
        }
        return 6 * 60 // Fallback
    }
}
