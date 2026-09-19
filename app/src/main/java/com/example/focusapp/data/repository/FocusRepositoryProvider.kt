package com.example.focusapp.data.repository

import android.content.Context
import com.example.focusapp.data.local.RoomLocalDataSource
import com.example.focusapp.data.remote.FirebaseRemoteDataSource
import com.example.focusapp.domain.repository.FocusRepository

/**
 * FocusRepositoryProvider
 * ---------------------------
 * A minimal, manual stand-in for a real dependency-injection framework
 * (Hilt, Koin, etc.), which this project doesn't have set up yet. Its
 * only job is "hand back the same [FocusRepository] instance every time,
 * creating it once on first use" - so every screen reads/writes the exact
 * same underlying [RoomLocalDataSource] (Room) and [FirebaseRemoteDataSource]
 * (Firebase) rather than each accidentally creating their own.
 *
 * STATUS IN THIS BRANCH: now wired up for real. HistoryViewModel (Local
 * data layer) and the Home/Apps/Map screens brought in from the team's
 * merged UI (see EditLocationZoneScreen, HomeScreenWithSheet,
 * AppsScreen, AddAppGroupScreen, EditAppGroupScreen, FocusSessionScreen)
 * all read/write through FocusRepositoryProvider.get(context).
 *
 * REQUIRES a real `app/google-services.json` (generated from
 * `google-services.json.template` + a `FIREBASE_API_KEY` in
 * local.properties - see app/build.gradle.kts's `generateGoogleServicesJson`
 * task) to actually reach Firebase at runtime. Without it, Room-backed
 * calls (zones/app groups/session history) still work fully offline;
 * only the Firebase-backed calls (session cloud-sync, Study Party) fail.
 */
object FocusRepositoryProvider {

    @Volatile
    private var instance: FocusRepository? = null

    fun get(context: Context): FocusRepository {
        return instance ?: synchronized(this) {
            instance ?: FocusRepositoryImpl(
                localDataSource = RoomLocalDataSource(context.applicationContext),
                remoteDataSource = FirebaseRemoteDataSource()
            ).also { instance = it }
        }
    }
}
