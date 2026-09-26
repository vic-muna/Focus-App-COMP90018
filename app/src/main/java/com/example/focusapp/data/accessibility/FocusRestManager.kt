package com.example.focusapp.data.accessibility

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * FocusRestManager
 * -------------------
 * [Claude, 2026-09-26] The "tea break" side of a focus session: how many
 * rests the session allows, how many are used, and the rest currently
 * running. During a rest, app blocking is lifted by clearing
 * [AccessibilityBridge]'s restricted set, and restored when the rest ends.
 *
 * A process-wide singleton for the same reason as [AccessibilityBridge]:
 * both NavGraph (which starts/ends sessions) and BlockedActivity (where the
 * user asks for a rest) need it, with no shared screen between them.
 */
object FocusRestManager {

    /** One session's rest allowance and progress. [restEndsAtMillis] is set while a rest runs. */
    data class RestState(
        val restsTotal: Int,
        val restsUsed: Int,
        val restMinutes: Int,
        val restEndsAtMillis: Long? = null,
    ) {
        val restsLeft: Int get() = (restsTotal - restsUsed).coerceAtLeast(0)
        val isResting: Boolean get() = restEndsAtMillis != null
    }

    private val _state = MutableStateFlow<RestState?>(null)

    /** null while no focus session is running. */
    val state: StateFlow<RestState?> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restJob: Job? = null
    private var pausedPackages: Set<String> = emptySet()

    /** Called when a focus session starts. */
    fun startSession(restsTotal: Int, restMinutes: Int) {
        endSession()
        _state.value = RestState(restsTotal = restsTotal, restsUsed = 0, restMinutes = restMinutes)
    }

    /** Called when a focus session ends; cancels any running rest without restoring blocking. */
    fun endSession() {
        restJob?.cancel()
        restJob = null
        pausedPackages = emptySet()
        _state.value = null
    }

    /**
     * Uses one rest: lifts blocking now and restores it after the session's
     * rest length, then calls [onRestEnded] with the packages blocked again.
     * Returns false (and does nothing) when no rest is available.
     */
    fun takeRest(onRestEnded: (restoredPackages: Set<String>) -> Unit = {}): Boolean {
        val current = _state.value ?: return false
        if (current.restsLeft <= 0 || current.isResting) return false

        pausedPackages = AccessibilityBridge.restrictedPackages.value
        AccessibilityBridge.clearRestrictedPackages()
        val restMillis = current.restMinutes * 60_000L
        _state.value = current.copy(
            restsUsed = current.restsUsed + 1,
            restEndsAtMillis = System.currentTimeMillis() + restMillis,
        )

        restJob = scope.launch {
            delay(restMillis)
            val restored = pausedPackages
            AccessibilityBridge.setRestrictedPackages(restored)
            pausedPackages = emptySet()
            _state.value = _state.value?.copy(restEndsAtMillis = null)
            onRestEnded(restored)
        }
        return true
    }
}
