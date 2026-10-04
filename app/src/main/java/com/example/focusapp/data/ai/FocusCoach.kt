package com.example.focusapp.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

/**
 * Focus Coach: short feedback on a week of focus, written by an AI from a
 * [com.example.focusapp.domain.model.WeeklyFocusSummary]'s text.
 * An interface so the screen can be tried out with a fake.
 */
interface FocusCoach {

    /** Feedback and suggestions on the week described by [summary]. */
    suspend fun weeklyReport(summary: String): String

    /** Answers [question] about the same week, with the [report] already given as context. */
    suspend fun followUp(summary: String, report: String, question: String): String

    /**
     * [Claude, 2026-10-04] One short line about a single just-finished focus session - the
     * per-session counterpart to [weeklyReport]. Deliberately on this same interface/class
     * (one Gemini setup, one App Check-verified client) rather than a second, separate AI
     * class - see this file's git history for the chat where that consolidation was decided.
     */
    suspend fun sessionFeedback(durationMinutes: Long, distractingAppOpenCount: Int, wasCompletedSuccessfully: Boolean): String
}

/**
 * [FocusCoach] backed by Gemini through Firebase AI Logic (Gemini Developer API, free plan).
 * No Gemini key is in the app - Firebase holds it, and App Check (see installAppCheck) proves
 * requests come from this app.
 */
class GeminiFocusCoach : FocusCoach {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(SYSTEM_INSTRUCTION) },
        )
    }

    // [Claude, 2026-10-04] A second GenerativeModel, same Firebase AI Logic setup and same
    // MODEL_NAME, but its own system instruction tailored to a one-line reply instead of the
    // weekly report's multi-bullet format - SYSTEM_INSTRUCTION above explicitly asks for
    // "3 to 5 lines", which a single just-finished session has nowhere near enough data to fill
    // sensibly. Two models in one class, both behind this same FocusCoach interface and the
    // same App Check-verified client, rather than two separate AI classes - see
    // sessionFeedback()'s own doc comment.
    private val sessionModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(SESSION_SYSTEM_INSTRUCTION) },
        )
    }

    override suspend fun weeklyReport(summary: String): String =
        ask(model, content(role = "user") { text(reportRequest(summary)) })

    override suspend fun followUp(summary: String, report: String, question: String): String = ask(
        model,
        content(role = "user") { text(reportRequest(summary)) },
        content(role = "model") { text(report) },
        content(role = "user") { text("$question Answer in under 100 words.") },
    )

    override suspend fun sessionFeedback(
        durationMinutes: Long,
        distractingAppOpenCount: Int,
        wasCompletedSuccessfully: Boolean
    ): String = ask(
        sessionModel,
        content(role = "user") {
            text(
                "Session just finished: $durationMinutes minutes, " +
                    "$distractingAppOpenCount distracting-app opens, " +
                    "completed in full: $wasCompletedSuccessfully."
            )
        },
    )

    private suspend fun ask(model: GenerativeModel, vararg turns: Content): String {
        val response = model.generateContent(turns.toList())
        val text = response.text?.let(::toPlainText)
        if (text.isNullOrBlank()) throw IllegalStateException("Focus Coach didn't send an answer.")
        return text
    }

    private fun reportRequest(summary: String) =
        "$summary\n\nThis is my history of focus sessions this week. Could you give me some feedback and suggestions?"

    private companion object {
        /** A stable Gemini model on the free plan (retires no earlier than May 2027 - check the
         *  Firebase AI Logic "models" page before then). */
        const val MODEL_NAME = "gemini-3.5-flash"

        val SYSTEM_INSTRUCTION = """
            You are Focus Coach, a friendly study coach inside a focus app that blocks distracting apps.
            The user shares statistics about their focus sessions. Base your feedback only on those numbers;
            never invent data. Be encouraging and practical.
            Write plain text only: no headings, bold, italics or tables.
            Start with one short sentence, then 3 to 5 lines starting with "- ", each pairing an observation
            with a concrete suggestion. Stay under 150 words.
            If there is little data, say so and suggest starting with short, regular sessions.
            Do not give medical or mental-health advice.
        """.trimIndent()

        val SESSION_SYSTEM_INSTRUCTION = """
            You are a short, encouraging line of feedback inside a focus app, shown right after
            one focus session ends. The user shares that one session's stats. Base your reply
            only on those numbers; never invent data.
            Write exactly ONE plain-text sentence, no more than about 20 words: no headings,
            bold, italics, bullets or quotation marks.
            Be warm but not over the top. Do not give medical or mental-health advice.
        """.trimIndent()
    }
}

/** Gemini sometimes answers in Markdown anyway - turn bold/italic marks and "*" bullets into plain text. */
internal fun toPlainText(text: String): String = text
    .replace("**", "")
    .replace("__", "")
    .lines()
    .joinToString("\n") { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("* ")) "- " + trimmed.removePrefix("* ") else line
    }
    .trim()
