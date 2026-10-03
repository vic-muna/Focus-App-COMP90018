package com.example.focusapp.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.ai.FocusCoach
import com.example.focusapp.data.ai.GeminiFocusCoach
import com.example.focusapp.data.preferences.CoachFollowUp
import com.example.focusapp.data.preferences.FocusCoachStorage
import com.example.focusapp.data.preferences.RewardSettingsStorage
import com.example.focusapp.data.preferences.SavedCoachReport
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.usecase.BuildWeeklyFocusSummaryUseCase
import com.example.focusapp.domain.usecase.CalculateFocusRewardUseCase
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** What the Focus Coach popup is showing. */
enum class CoachStage {
    LOADING,
    /** No sessions in the last 7 days - nothing to give feedback on (and no request is spent). */
    NO_SESSIONS,
    /** First use: asking whether the focus summary may be sent to Gemini. */
    CONSENT,
    READY,
}

data class FocusCoachState(
    val stage: CoachStage = CoachStage.LOADING,
    /** Null until the report has been asked for. */
    val report: String? = null,
    val followUps: List<CoachFollowUp> = emptyList(),
    /** True while waiting for Focus Coach's answer. */
    val isThinking: Boolean = false,
    val error: String? = null,
    val requestsLeft: Int = FocusCoachStorage.DAILY_LIMIT,
) {
    /** The preset questions not asked yet. */
    val questionsLeft: List<String> get() = PRESET_QUESTIONS - followUps.map { it.question }.toSet()

    val canAsk: Boolean get() = !isThinking && requestsLeft > 0
}

/** The follow-up questions offered after the report. */
val PRESET_QUESTIONS = listOf(
    "How can I reduce distractions?",
    "When is my best time to focus?",
    "How do I keep my streak going?",
)

/**
 * Focus Coach on the dashboard: a one-off weekly report from Gemini, then preset follow-up
 * questions. Each answer is one AI request; [FocusCoachStorage.DAILY_LIMIT] are allowed a day and
 * failed ones don't count. Today's report is saved, so reopening the popup doesn't ask again.
 */
class FocusCoachViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = FocusCoachStorage(application)
    private val rewardSettings = RewardSettingsStorage(application)
    private val coach: FocusCoach by lazy { GeminiFocusCoach() }

    /** The summary text of the last 7 days, worked out in [open]. */
    private var summary: String = ""

    private val _state = MutableStateFlow(FocusCoachState())
    val state: StateFlow<FocusCoachState> = _state.asStateFlow()

    /** Called every time the popup opens: works out this week's summary and shows any saved report for it. */
    fun open() {
        viewModelScope.launch {
            _state.value = FocusCoachState(stage = CoachStage.LOADING, requestsLeft = requestsLeft())
            try {
                val sessions = FocusRepositoryProvider.get(getApplication()).getSessionHistory()
                val rewards = CalculateFocusRewardUseCase().execute(sessions, rewardSettings.getStreakGoalMinutes())
                val weekly = BuildWeeklyFocusSummaryUseCase().execute(sessions, rewards)
                summary = weekly.toPromptText()
                val saved = storage.getReportFor(summary)
                _state.value = FocusCoachState(
                    stage = when {
                        !weekly.hasSessionsThisWeek -> CoachStage.NO_SESSIONS
                        !storage.hasConsent() -> CoachStage.CONSENT
                        else -> CoachStage.READY
                    },
                    report = saved?.report,
                    followUps = saved?.followUps.orEmpty(),
                    requestsLeft = requestsLeft(),
                )
            } catch (e: Exception) {
                _state.value = FocusCoachState(
                    stage = CoachStage.READY,
                    error = "Couldn't read your focus history. Try again.",
                    requestsLeft = requestsLeft(),
                )
            }
        }
    }

    fun acceptConsent() {
        storage.saveConsent()
        _state.update { it.copy(stage = CoachStage.READY) }
    }

    fun requestReport() = runRequest { current ->
        val report = coach.weeklyReport(summary)
        storage.saveReport(SavedCoachReport(summary, report, emptyList()))
        current.copy(report = report, followUps = emptyList())
    }

    fun ask(question: String) {
        val report = _state.value.report ?: return
        runRequest { current -> askAbout(current, report, question) }
    }

    private suspend fun askAbout(current: FocusCoachState, report: String, question: String): FocusCoachState {
        val answer = coach.followUp(summary, report, question)
        val followUps = current.followUps + CoachFollowUp(question, answer)
        storage.saveReport(SavedCoachReport(summary, report, followUps))
        return current.copy(followUps = followUps)
    }

    /** Runs one AI request: counts it only if it worked, and shows an error if it didn't. */
    private fun runRequest(request: suspend (FocusCoachState) -> FocusCoachState) {
        val current = _state.value
        if (!current.canAsk || summary.isEmpty()) return
        _state.value = current.copy(isThinking = true, error = null)
        viewModelScope.launch {
            _state.value = try {
                val updated = request(current)
                storage.recordRequest()
                updated.copy(isThinking = false, requestsLeft = requestsLeft())
            } catch (e: Exception) {
                current.copy(isThinking = false, error = coachErrorMessage(e))
            }
        }
    }

    private fun requestsLeft(): Int = (FocusCoachStorage.DAILY_LIMIT - storage.requestsUsedToday()).coerceAtLeast(0)
}

private fun coachErrorMessage(e: Throwable): String = when {
    e is QuotaExceededException ->
        "Focus Coach is busy right now (the free Gemini quota is used up). Try again later."
    e is PromptBlockedException || e is ResponseStoppedException || e is ContentBlockedException ->
        "Focus Coach couldn't answer that one. Try another question."
    e is ServiceDisabledException || e is APINotConfiguredException ->
        "Focus Coach isn't set up yet: turn on Firebase AI Logic (Gemini Developer API) in the Firebase console."
    e is UnsupportedUserLocationException ->
        "Focus Coach isn't available in your region."
    generateSequence(e) { it.cause }.any { it is IOException } ->
        "No internet connection. Check your connection and try again."
    else ->
        "Focus Coach couldn't answer (${e::class.java.simpleName}). Try again in a moment."
}
