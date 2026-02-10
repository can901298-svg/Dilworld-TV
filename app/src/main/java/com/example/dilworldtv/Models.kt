package com.example.dilworldtv

data class ScheduleBlock(
    val start: String,
    val end: String,
    val type: String,
    val label: String? = null
)

data class ScheduleState(
    val nowType: String,
    val nowLabel: String,
    val minutesLeft: Int,
    val nextLabel: String
)

data class DutySlot(
    val start: String,
    val end: String,
    val teacher: String
)

data class DutyArea(
    val area: String,
    val teacher: String? = null,
    val allBreaks: Boolean? = null,
    val slots: List<DutySlot>? = null
)

data class DutyUi(
    val area: String,
    val current: String?,
    val next: String?,
    val nextInMinutes: Int?
)

data class WeatherUi(
    val tempC: Int,
    val descriptionTr: String
)

data class FxUi(
    val usd: String?,
    val eur: String?,
    val gramGoldTry: String?,
    val asOfDate: String?
)

data class NewsUi(
    val titles: List<String>,
    val fetchedAtIso: String
)
