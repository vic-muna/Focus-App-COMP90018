package com.example.focusapp.ui.screens.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.preferences.RewardSettingsStorage
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.RewardProgress
import com.example.focusapp.domain.usecase.CalculateFocusRewardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Works out [RewardsScreen]'s milestones and streak from the saved focus sessions. */
class RewardsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = RewardSettingsStorage(application)
    private val calculateReward = CalculateFocusRewardUseCase()

    private val _progress = MutableStateFlow<RewardProgress?>(null)

    /** Null while loading. */
    val progress: StateFlow<RewardProgress?> = _progress.asStateFlow()

    /** (Re)works out the rewards - called every time the screen is shown, so a new day or a changed goal shows up. */
    fun load() {
        viewModelScope.launch {
            val sessions = FocusRepositoryProvider.get(getApplication()).getSessionHistory()
            _progress.value = calculateReward.execute(sessions, settings.getStreakGoalMinutes())
        }
    }
}
