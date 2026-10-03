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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = FocusPreferences(application)
    
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _newlyUnlockedReward = MutableStateFlow<RewardItem?>(null)
    val newlyUnlockedReward: StateFlow<RewardItem?> = _newlyUnlockedReward.asStateFlow()

    private val _earnedPointsNotification = MutableStateFlow<Int?>(null)
    val earnedPointsNotification: StateFlow<Int?> = _earnedPointsNotification.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartMillis: Long = 0L
    private var sessionTotalMinutes: Int = 0

    val totalHours = prefs.totalFocusHours
    val focusPoints = prefs.focusPoints
    val unlockedIds = prefs.unlockedRewardIds

    val availableBackgrounds = RewardPresets.ALL_BACKGROUNDS

    val selectedBackground: StateFlow<RewardItem> = prefs.selectedBackgroundId
        .map { id ->
            RewardPresets.ALL_BACKGROUNDS.find { it.id == id } ?: RewardPresets.BACKGROUND_FOREST
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RewardPresets.BACKGROUND_FOREST
        )

    fun selectBackground(background: RewardItem) {
        viewModelScope.launch {
            prefs.setSelectedBackground(background.id)
        }
    }

    fun unlockBackgroundWithPoints(
        background: RewardItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val currentPoints = focusPoints.first()
            val cost = background.costPoints
            if (currentPoints >= cost) {
                val success = prefs.spendPointsAndUnlock(cost, background.id)
                if (success) {
                    prefs.setSelectedBackground(background.id)
                    onSuccess()
                } else {
                    onError("Not enough points!")
                }
            } else {
                onError("Need $cost points to unlock! (Current: $currentPoints pts)")
            }
        }
    }

    fun startTimer(durationMinutes: Int) {
        _timerSeconds.value = durationMinutes * 60
        _isTimerRunning.value = true
        sessionStartMillis = System.currentTimeMillis()
        sessionTotalMinutes = durationMinutes
        
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
        prefs.addPoints(durationMinutes)
        _earnedPointsNotification.value = durationMinutes
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

    fun clearEarnedPointsNotification() {
        _earnedPointsNotification.value = null
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        
        if (sessionStartMillis > 0) {
            val elapsedSeconds = ((System.currentTimeMillis() - sessionStartMillis) / 1000).coerceAtLeast(0)
            val elapsedMinutes = (elapsedSeconds / 60).toInt()
            if (elapsedMinutes >= 1) {
                viewModelScope.launch {
                    val hoursAdded = elapsedMinutes / 60f
                    prefs.addFocusTime(hoursAdded)
                    prefs.addPoints(elapsedMinutes)
                    _earnedPointsNotification.value = elapsedMinutes
                    checkForUnlocks()
                }
            }
        }
        sessionStartMillis = 0L
    }
}