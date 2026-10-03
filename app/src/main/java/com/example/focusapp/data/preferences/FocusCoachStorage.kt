package com.example.focusapp.data.preferences

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One preset question asked after the report, with Focus Coach's answer. */
data class CoachFollowUp(val question: String, val answer: String)

/** Today's report and follow-ups, kept so reopening Focus Coach doesn't ask again. */
data class SavedCoachReport(val summary: String, val report: String, val followUps: List<CoachFollowUp>)

/**
 * Focus Coach's state on this phone:
 *  - whether the user agreed to send their focus summary to Gemini
 *  - how many AI requests were used today ([DAILY_LIMIT] a day, back to 0 at midnight)
 *  - the last report and its follow-ups, for the summary they were written about
 * The daily limit is only kept on this phone, so reinstalling the app resets it.
 */
class FocusCoachStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasConsent(): Boolean = prefs.getBoolean(KEY_CONSENT, false)

    fun saveConsent() {
        prefs.edit().putBoolean(KEY_CONSENT, true).apply()
    }

    fun requestsUsedToday(): Int =
        if (prefs.getString(KEY_USAGE_DAY, null) == today()) prefs.getInt(KEY_USAGE_COUNT, 0) else 0

    /** Counts one more AI request for today. */
    fun recordRequest() {
        prefs.edit()
            .putString(KEY_USAGE_DAY, today())
            .putInt(KEY_USAGE_COUNT, requestsUsedToday() + 1)
            .apply()
    }

    /** The saved report, only if it was written about this exact [summary] (same week, same sessions). */
    fun getReportFor(summary: String): SavedCoachReport? {
        val json = prefs.getString(KEY_REPORT, null) ?: return null
        return try {
            val saved = JSONObject(json)
            if (saved.getString("summary") != summary) return null
            val followUps = saved.getJSONArray("followUps")
            SavedCoachReport(
                summary = summary,
                report = saved.getString("report"),
                followUps = (0 until followUps.length()).map {
                    val item = followUps.getJSONObject(it)
                    CoachFollowUp(item.getString("question"), item.getString("answer"))
                },
            )
        } catch (e: Exception) {
            null // Unreadable - just ask again.
        }
    }

    fun saveReport(saved: SavedCoachReport) {
        val followUps = JSONArray()
        saved.followUps.forEach { followUps.put(JSONObject().put("question", it.question).put("answer", it.answer)) }
        val json = JSONObject()
            .put("summary", saved.summary)
            .put("report", saved.report)
            .put("followUps", followUps)
        prefs.edit().putString(KEY_REPORT, json.toString()).apply()
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        /** AI requests (reports and follow-ups) allowed per day. */
        const val DAILY_LIMIT = 5

        private const val PREFS_NAME = "focus_coach"
        private const val KEY_CONSENT = "consent"
        private const val KEY_USAGE_DAY = "usage_day"
        private const val KEY_USAGE_COUNT = "usage_count"
        private const val KEY_REPORT = "report"
    }
}
