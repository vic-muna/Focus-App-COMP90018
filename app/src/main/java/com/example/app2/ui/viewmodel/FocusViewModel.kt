package com.example.app2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app2.data.FocusPreferences
import com.example.app2.model.RewardItem
import com.example.app2.model.RewardPresets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = FocusPreferences(application)
    
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _newlyUnlockedReward = MutableStateFlow<RewardItem?>(null)
    val newlyUnlockedReward: StateFlow<RewardItem?> = _newlyUnlockedReward.asStateFlow()

    private var timerJob: Job? = null

    val totalHours = prefs.totalFocusHours
    val unlockedIds = prefs.unlockedRewardIds

    fun startTimer(durationMinutes: Int) {
        _timerSeconds.value = durationMinutes * 60
        _isTimerRunning.value = true
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0) {
                delay(1000)
                _timerSeconds.value -= 1
            }
            onTimerFinished(durationMinutes)
        }
    }

    private suspend fun onTimerFinished(durationMinutes: Int) {
        _isTimerRunning.value = false
        val hoursAdded = durationMinutes / 60f
        prefs.addFocusTime(hoursAdded)
        checkForUnlocks()
    }

    private suspend fun checkForUnlocks() {
        val currentTotal = totalHours.first()
        val currentlyUnlocked = unlockedIds.first()
        
        RewardPresets.ALL_REWARDS.forEach { reward ->
            if (currentTotal >= reward.thresholdHours && !currentlyUnlocked.contains(reward.id)) {
                prefs.unlockReward(reward.id)
                _newlyUnlockedReward.value = reward
            }
        }
    }

    fun dismissUnlockDialog() {
        _newlyUnlockedReward.value = null
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
    }
}