package com.example.dilworldtv

import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleEngine(private val jsonText: String) {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")
    private val data = JSONObject(jsonText).getJSONObject("days")

    private fun keyFor(day: DayOfWeek): String = when (day) {
        DayOfWeek.FRIDAY -> "FRI"
        DayOfWeek.SATURDAY -> "SAT"
        DayOfWeek.SUNDAY -> "SUN"
        else -> "NONE"
    }

    fun compute(now: LocalDateTime): ScheduleState? {
        val dayKey = keyFor(now.dayOfWeek)
        if (!data.has(dayKey)) return null

        val arr = data.getJSONArray(dayKey)
        val blocks = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ScheduleBlock(
                start = o.getString("start"),
                end = o.getString("end"),
                type = o.getString("type"),
                label = o.optString("label", o.getString("type"))
            )
        }

        val tNow = now.toLocalTime()

        for (i in blocks.indices) {
            val b = blocks[i]
            val s = LocalTime.parse(b.start, fmt)
            val e = LocalTime.parse(b.end, fmt)
            if (!tNow.isBefore(s) && tNow.isBefore(e)) {
                val minsLeft = java.time.Duration.between(tNow, e).toMinutes().toInt()
                val next = blocks.getOrNull(i + 1)
                val nextLabel = next?.label ?: "Gün Tamamlandı"
                val nowLabel = when (b.type) {
                    "DERS" -> b.label ?: "Ders"
                    "TENEFUS" -> "Teneffüs"
                    "OGLE" -> "Öğle Arası"
                    "ARA" -> b.label ?: "Ara"
                    else -> b.label ?: b.type
                }
                return ScheduleState(
                    nowType = b.type,
                    nowLabel = nowLabel,
                    minutesLeft = minsLeft.coerceAtLeast(0),
                    nextLabel = nextLabel
                )
            }
        }

        val next = blocks.firstOrNull {
            val s = LocalTime.parse(it.start, fmt)
            tNow.isBefore(s)
        } ?: return ScheduleState("OFF", "Gün Tamamlandı", 0, "—")

        val sNext = LocalTime.parse(next.start, fmt)
        val mins = java.time.Duration.between(tNow, sNext).toMinutes().toInt()

        return ScheduleState(
            nowType = "OFF",
            nowLabel = "Başlıyor: ${next.label}",
            minutesLeft = mins.coerceAtLeast(0),
            nextLabel = next.label
        )
    }
}
