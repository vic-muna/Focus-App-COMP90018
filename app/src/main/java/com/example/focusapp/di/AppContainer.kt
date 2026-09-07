package com.example.focusapp.di

import android.content.Context
import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.data.remote.FirebaseRemoteDataSource
import com.example.focusapp.data.repository.FocusRepositoryImpl
import com.example.focusapp.domain.repository.FocusRepository

/**
 * AppContainer
 * ---------------
 * Minimal manual dependency wiring - there's no Hilt/Koin in this project
 * yet, and adding one is out of scope here. Call
 * AppContainer.focusRepository(context) wherever a ViewModel needs a
 * FocusRepository, e.g. inside a ViewModelProvider.Factory once
 * HistoryViewModel / FocusModeViewModel / RewardsViewModel actually take
 * one as a constructor argument (they don't yet - that's UI-layer work,
 * not touched here so it doesn't collide with whoever owns those screens).
 */
object AppContainer {
    fun focusRepository(context: Context): FocusRepository =
        FocusRepositoryImpl(
            localDataSource = LocalDataSource(context),
            remoteDataSource = FirebaseRemoteDataSource()
        )
}
