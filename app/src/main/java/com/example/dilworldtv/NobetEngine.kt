package com.example.dilworldtv

import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NobetEngine(private val jsonText: String) {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")
    private val data = JSONObject(jsonText).getJSONObject("days")

    private fun keyFor(day: DayOfWeek): String = when (day) {
        DayOfWeek.FRIDAY -> "FRI"
        DayOfWeek.SATURDAY -> "SAT"
        DayOfWeek.SUNDAY -> "SUN"
        else -> "NONE"
    }

    fun compute(now: LocalDateTime, currentScheduleType: String): List<DutyUi> {
        val dayKey = keyFor(now.dayOfWeek)
        if (!data.has(dayKey)) return emptyList()

        val tNow = now.toLocalTime()
        val arr = data.getJSONArray(dayKey)
        val areas = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            DutyArea(
                area = o.getString("area"),
                teacher = o.optString("teacher", null),
                allBreaks = if (o.has("allBreaks")) o.getBoolean("allBreaks") else null,
                slots = if (o.has("slots")) {
                    val sArr = o.getJSONArray("slots")
                    (0 until sArr.length()).map { j ->
                        val so = sArr.getJSONObject(j)
                        DutySlot(so.getString("start"), so.getString("end"), so.getString("teacher"))
                    }
                } else null
            )
        }

        return areas.map { area ->
            if (area.allBreaks == true) {
                val show = (currentScheduleType == "TENEFUS")
                return@map DutyUi(
                    area = area.area,
                    current = if (show) area.teacher else null,
                    next = if (!show) area.teacher else null,
                    nextInMinutes = null
                )
            }

            val slots = area.slots.orEmpty()
            val cur = slots.firstOrNull {
                val s = LocalTime.parse(it.start, fmt)
                val e = LocalTime.parse(it.end, fmt)
                !tNow.isBefore(s) && tNow.isBefore(e)
            }

            if (cur != null) {
                DutyUi(area.area, current = cur.teacher, next = null, nextInMinutes = null)
            } else {
                val next = slots.firstOrNull {
                    val s = LocalTime.parse(it.start, fmt)
                    tNow.isBefore(s)
                }
                if (next == null) DutyUi(area.area, null, null, null)
                else {
                    val s = LocalTime.parse(next.start, fmt)
                    val mins = java.time.Duration.between(tNow, s).toMinutes().toInt()
                    DutyUi(area.area, null, next.teacher, mins.coerceAtLeast(0))
                }
            }
        }
    }
}
