package com.example.focusapp.ui.screens.timefocus

import android.content.Context
import com.example.focusapp.ui.screens.home.ClockTime
import com.example.focusapp.ui.screens.home.TimeSlot
import org.json.JSONArray
import org.json.JSONObject

/** One entry of the Time Focus list: a repeating time range that can be switched on/off. */
data class FocusTimeSlot(
    val id: String,
    val slot: TimeSlot,
    val enabled: Boolean = true,
)

/**
 * TimeSlotStorage
 * ------------------
 * [Claude, 2026-09-27] Persists the Time Focus list (SharedPreferences +
 * JSON, the same approach as BlockedAppGroupStorage). UI-side only: the
 * data layer has no standalone time-slot model, and nothing triggers focus
 * sessions from these yet - schedules that do trigger sessions still live
 * on each BlockedAppGroup.
 */
class TimeSlotStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSlots(): List<FocusTimeSlot> {
        val json = prefs.getString(KEY_SLOTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { slotFromJson(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun saveSlots(slots: List<FocusTimeSlot>) {
        val array = JSONArray()
        slots.forEach { array.put(slotToJson(it)) }
        prefs.edit().putString(KEY_SLOTS, array.toString()).apply()
    }

    private fun slotToJson(item: FocusTimeSlot) = JSONObject().apply {
        put("id", item.id)
        put("enabled", item.enabled)
        put("activeDays", JSONArray(item.slot.activeDays.toList()))
        put("startHour", item.slot.start.hour)
        put("startMinute", item.slot.start.minute)
        put("endHour", item.slot.end.hour)
        put("endMinute", item.slot.end.minute)
    }

    private fun slotFromJson(obj: JSONObject): FocusTimeSlot {
        val days = obj.getJSONArray("activeDays")
        return FocusTimeSlot(
            id = obj.getString("id"),
            enabled = obj.optBoolean("enabled", true),
            slot = TimeSlot(
                activeDays = (0 until days.length()).map { days.getString(it) }.toSet(),
                start = ClockTime(obj.getInt("startHour"), obj.getInt("startMinute")),
                end = ClockTime(obj.getInt("endHour"), obj.getInt("endMinute")),
            ),
        )
    }

    private companion object {
        const val PREFS_NAME = "focus_time_slots"
        const val KEY_SLOTS = "slots_json"
    }
}
