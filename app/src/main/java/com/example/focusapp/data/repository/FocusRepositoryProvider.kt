package com.example.focusapp.data.repository

import android.content.Context
import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.data.remote.RemoteDataSource
import com.example.focusapp.domain.repository.FocusRepository

/**
 * FocusRepositoryProvider
 * ---------------------------
 * A minimal, manual stand-in for a real dependency-injection framework
 * (Hilt, Koin, etc.), which this project doesn't have set up yet. Its
 * only job is "hand back the same [FocusRepository] instance every time,
 * creating it once on first use" - so that, for example, AppsScreen and
 * AddAppGroupScreen are reading/writing the exact same underlying
 * SharedPreferences-backed [LocalDataSource] rather than each accidentally
 * creating their own.
 *
 * [RemoteDataSource] has no real implementation yet (the REST-vs-Firebase
 * decision is still open - see that interface's doc comment), but since
 * it declares zero functions, `object : RemoteDataSource {}` is a valid,
 * genuinely empty no-op implementation - just enough to satisfy
 * [com.example.focusapp.data.repository.FocusRepositoryImpl]'s
 * constructor without inventing a fake backend.
 *
 * TODO: to be implemented later - delete this file entirely once a real
 * DI framework is added; this whole pattern (a manually-written singleton
 * holder) is exactly what those frameworks generate for you.
 */
object FocusRepositoryProvider {

    @Volatile
    private var instance: FocusRepository? = null

    /**
     * Returns the app-wide [FocusRepository], creating it on the first
     * call. [context] only needs to be any Context (an Activity, an
     * Application, etc.) - internally, [LocalDataSource] immediately
     * switches to `.applicationContext`, so nothing here can leak one.
     */
    fun get(context: Context): FocusRepository {
        return instance ?: synchronized(this) {
            instance ?: FocusRepositoryImpl(
                localDataSource = LocalDataSource(context.applicationContext),
                remoteDataSource = object : RemoteDataSource {}
            ).also { instance = it }
        }
    }
}
