package com.example.focusapp.data.repository

import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.data.remote.RemoteDataSource
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.repository.FocusRepository

/**
 * FocusRepositoryImpl
 * ----------------------
 * Concrete implementation of [FocusRepository]. Combines [LocalDataSource]
 * (offline cache) and [RemoteDataSource] (cloud sync, used by the Study
 * Party feature), per the "Data Layer" section of the project plan.
 *
 * This class is written to depend only on the RemoteDataSource interface,
 * not on Firebase or Retrofit directly, so the REST-vs-Firebase decision
 * (still open - see RemoteDataSource.kt) does not require any changes here.
 *
 * For now every method just delegates to LocalDataSource and ignores
 * RemoteDataSource entirely (no network calls happen yet).
 *
 * TODO: to be implemented later - add real sync logic with
 * RemoteDataSource once the backend choice is made.
 */
class FocusRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : FocusRepository {

    override suspend fun getFocusZones(): List<FocusZone> {
        return localDataSource.getFocusZones()
        // TODO: to be implemented later - merge with remoteDataSource results.
    }

    override suspend fun saveFocusZone(zone: FocusZone) {
        localDataSource.saveFocusZone(zone)
        // TODO: to be implemented later - also push to remoteDataSource.
    }

    override suspend fun getAppGroups(): List<AppGroup> {
        return localDataSource.getAppGroups()
        // TODO: to be implemented later - merge with remoteDataSource results.
    }

    override suspend fun saveAppGroup(group: AppGroup) {
        localDataSource.saveAppGroup(group)
        // TODO: to be implemented later - also push to remoteDataSource.
    }

    override suspend fun getSessionHistory(): List<FocusSession> {
        return localDataSource.getSessionHistory()
    }

    override suspend fun saveFocusSession(session: FocusSession) {
        localDataSource.saveFocusSession(session)
        // TODO: to be implemented later - also push to remoteDataSource
        // (needed so the reward/streak state syncs across devices).
    }
}
