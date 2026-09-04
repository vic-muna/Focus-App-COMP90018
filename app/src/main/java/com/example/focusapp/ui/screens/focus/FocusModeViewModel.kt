package com.example.focusapp.ui.screens.focus

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FocusModeUiState
 * ------------------
 * Everything [FocusModeScreen] needs in order to render itself. Extend
 * this with real fields (active focus-zone name, remaining time, etc.)
 * as those features are implemented.
 */
data class FocusModeUiState(
    val statusLabel: String = "Not Active"
)

/**
 * FocusModeViewModel
 * --------------------
 * Holds UI state for the Focus Mode screen. This is the "UI Layer" piece
 * described in the project plan (Activity/Fragment + ViewModel).
 *
 * NONE of the real business logic lives here yet. In particular:
 *  - Deciding whether the user is inside a focus zone / time slot belongs
 *    in the Domain layer (see domain/usecase/EvaluateFocusTriggerUseCase.kt).
 *  - Reading GPS / accelerometer / gyroscope sensors belongs in the Data
 *    layer (see data/sensor/SensorDataSource.kt).
 *  - Actually blocking apps / notifications is not implemented anywhere yet.
 *
 * TODO: to be implemented later - inject FocusRepository / use cases here
 * (e.g. via a constructor + a ViewModelProvider.Factory) once they do
 * something real.
 */
class FocusModeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FocusModeUiState())

    /** Read-only UI state observed by [FocusModeScreen]. */
    val uiState: StateFlow<FocusModeUiState> = _uiState.asStateFlow()

    /**
     * Called when the user taps the "Start Focus" button.
     *
     * TODO: to be implemented later:
     *  1. Call EvaluateFocusTriggerUseCase to check location/time rules.
     *  2. Start listening to relevant sensors via SensorDataSource.
     *  3. Begin blocking restricted apps/notifications.
     *
     * For now this just changes a placeholder status label so the screen
     * visibly reacts to the tap, without implementing any real behaviour.
     */
    fun onStartFocusClicked() {
        _uiState.value = _uiState.value.copy(
            statusLabel = "Active (placeholder only - no real logic implemented yet)"
        )
    }
}
