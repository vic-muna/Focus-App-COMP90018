package com.example.focusapp.ui.screens.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.preferences.BackgroundThemeStorage
import com.example.focusapp.data.preferences.FocusMusicStorage
import com.example.focusapp.data.preferences.RewardSettingsStorage
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusMusic
import com.example.focusapp.domain.model.RewardProgress
import com.example.focusapp.domain.usecase.CalculateFocusRewardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What [RewardsScreen] shows once loaded. */
data class RewardsState(
    val progress: RewardProgress,
    /** Backgrounds and music tracks bought (or unlocked with the test code). */
    val ownedIds: Set<String>,
    /** The track Focus Mode plays. */
    val selectedMusicId: String?,
)

/** Works out [RewardsScreen]'s points, milestones and shop from the saved focus sessions. */
class RewardsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = RewardSettingsStorage(application)
    private val themeStorage = BackgroundThemeStorage(application)
    private val musicStorage = FocusMusicStorage(application)
    private val calculateReward = CalculateFocusRewardUseCase()

    private val _state = MutableStateFlow<RewardsState?>(null)

    /** Null while loading. */
    val state: StateFlow<RewardsState?> = _state.asStateFlow()

    /** (Re)works out the rewards - called every time the screen is shown, so new focus time shows up. */
    fun load() {
        viewModelScope.launch {
            val sessions = FocusRepositoryProvider.get(getApplication()).getSessionHistory()
            _state.value = RewardsState(
                progress = calculateReward.execute(sessions, settings.getStreakGoalMinutes(), settings.getSpentPoints()),
                ownedIds = settings.getOwnedIds() + themeStorage.getCodeUnlockedIds(),
                selectedMusicId = musicStorage.getSelectedId(),
            )
        }
    }

    /** Spends points on [id] if there are enough and it isn't owned yet. */
    fun buy(id: String, pricePoints: Int) {
        val current = _state.value ?: return
        if (id in current.ownedIds || current.progress.points < pricePoints) return
        settings.buy(id, pricePoints)
        load()
    }

    /** Makes [music] the Focus Mode track and turns Focus Music on. */
    fun selectMusic(music: FocusMusic) {
        musicStorage.saveSelectedId(music.id)
        musicStorage.saveEnabled(true)
        _state.value = _state.value?.copy(selectedMusicId = music.id)
    }
}
