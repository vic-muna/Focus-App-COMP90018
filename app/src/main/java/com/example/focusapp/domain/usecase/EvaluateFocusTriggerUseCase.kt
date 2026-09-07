package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.FocusZone

/**
 * EvaluateFocusTriggerUseCase
 * ------------------------------
 * Domain-layer rule: decides whether Focus Mode should automatically
 * activate right now, based on location match AND/OR time-slot match
 * AND/OR a manual gesture (shake / face-down) - matches the
 * "Trigger (Work Independently)" node in the Runtime-mode part of the
 * process-flow diagram.
 *
 * This class currently contains NO real logic. It exists so the ViewModel
 * layer has a stable function to call once real sensor/location data
 * sources are implemented.
 *
 * TODO: to be implemented later:
 *  - Take a SensorDataSource + FocusRepository (constructor injection).
 *  - Check current location against saved FocusZones (Geofencing/Wi-Fi).
 *  - Check current time against saved time slots (AlarmManager).
 *  - Check for shake / face-down gesture events.
 */
class EvaluateFocusTriggerUseCase {

    /**
     * @param currentZones the user's saved focus zones to check against.
     * @return true if Focus Mode should be active right now.
     *
     * TODO: to be implemented later - always returns false for now.
     */
    fun execute(currentZones: List<FocusZone> = emptyList()): Boolean {
        return false
    }
}
