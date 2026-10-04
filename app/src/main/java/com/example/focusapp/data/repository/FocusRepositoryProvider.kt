package com.example.focusapp.data.repository

import android.content.Context
import com.example.focusapp.data.local.RoomLocalDataSource
import com.example.focusapp.data.remote.FirebaseRemoteDataSource
import com.example.focusapp.domain.repository.FocusRepository

/**
 * Gives every screen the same [FocusRepository], created once on first use.
 * Room (on the phone) always works; Firebase calls need app/google-services.json.
 */
object FocusRepositoryProvider {

    @Volatile
    private var instance: FocusRepository? = null

    fun get(context: Context): FocusRepository {
        return instance ?: synchronized(this) {
            instance ?: FocusRepositoryImpl(
                localDataSource = RoomLocalDataSource(context.applicationContext),
                remoteDataSource = FirebaseRemoteDataSource(),
                context = context.applicationContext
            ).also { instance = it }
        }
    }
}
