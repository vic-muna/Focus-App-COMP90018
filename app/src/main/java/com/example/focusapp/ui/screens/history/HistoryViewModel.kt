package com.example.focusapp.ui.screens.history

import androidx.lifecycle.ViewModel

/**
 * HistoryViewModel
 * ------------------
 * Will eventually expose a StateFlow of past focus sessions, loaded via
 * FocusRepository.getSessionHistory() (see
 * domain/repository/FocusRepository.kt), for [HistoryScreen] to display
 * in a scrollable list.
 *
 * TODO: to be implemented later - intentionally empty for now, no real
 * data loading happens yet.
 */
class HistoryViewModel : ViewModel()
