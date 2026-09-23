package com.samuelbaldasso.financeflow.domain.usecase.recurrence

import com.samuelbaldasso.financeflow.core.model.recurrence.RecurrenceFrequency
import com.samuelbaldasso.financeflow.core.model.recurrence.RecurrenceRule
import java.time.LocalDate

object RecurrenceEngine {

    fun generateOccurrences(
        rule: RecurrenceRule,
        windowEnd: LocalDate
    ): List<LocalDate> {
        val occurrences = mutableListOf<LocalDate>()
        var current = rule.startDate
        var count = 0

        val originalDayOfMonth = rule.startDate.dayOfMonth

        while (!current.isAfter(windowEnd)) {
            val endDate = rule.endDate
            if (endDate != null && current.isAfter(endDate)) break
            val maxOccurrences = rule.maxOccurrences
            if (maxOccurrences != null && count >= maxOccurrences) break

            occurrences.add(current)
            count++

            current = when (rule.frequency) {
                RecurrenceFrequency.DAILY -> current.plusDays(rule.interval.toLong())
                RecurrenceFrequency.WEEKLY -> current.plusWeeks(rule.interval.toLong())
                RecurrenceFrequency.MONTHLY -> {
                    // Safe month step preserving day of month with clamping (e.g. 31 -> 28/29/30)
                    val nextMonth = current.plusMonths(rule.interval.toLong())
                    val maxDayInNextMonth = nextMonth.lengthOfMonth()
                    val targetDay = minOf(originalDayOfMonth, maxDayInNextMonth)
                    nextMonth.withDayOfMonth(targetDay)
                }
                RecurrenceFrequency.YEARLY -> {
                    val nextYear = current.plusYears(rule.interval.toLong())
                    val maxDayInYear = nextYear.lengthOfMonth()
                    val targetDay = minOf(originalDayOfMonth, maxDayInYear)
                    nextYear.withDayOfMonth(targetDay)
                }
            }
        }

        return occurrences
    }
}
