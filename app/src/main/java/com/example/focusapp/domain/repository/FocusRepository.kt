package com.example.focusapp.domain.repository

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone

/**
 * FocusRepository
 * ------------------
 * The single source of truth that the Domain/UI layers talk to. The real
 * implementation ([com.example.focusapp.data.repository.FocusRepositoryImpl])
 * is meant to combine Local, Remote, and Sensor data sources, per the
 * "Data Layer" description in the project plan.
 *
 * All methods are stubs for now.
 * TODO: to be implemented later.
 */
interface FocusRepository {

    /** TODO: to be implemented later - read saved focus zones from local storage. */
    suspend fun getFocusZones(): List<FocusZone>

    /** TODO: to be implemented later - persist a new/edited focus zone. */
    suspend fun saveFocusZone(zone: FocusZone)

    /** TODO: to be implemented later - read saved app groups from local storage. */
    suspend fun getAppGroups(): List<AppGroup>

    /** TODO: to be implemented later - persist a new/edited app group. */
    suspend fun saveAppGroup(group: AppGroup)

    /** TODO: to be implemented later - read past focus sessions for the History screen. */
    suspend fun getSessionHistory(): List<FocusSession>

    /** TODO: to be implemented later - persist a completed focus session. */
    suspend fun saveFocusSession(session: FocusSession)
}
