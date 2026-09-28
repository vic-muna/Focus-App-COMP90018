package com.example.focusapp.domain.validation

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusZone

/**
 * Checks a zone or app group before it is saved (e.g. no blank name).
 * Plain Kotlin, so it runs in fast unit tests.
 */
object FocusValidation {

    /** @throws IllegalArgumentException if [group] isn't safe to persist. */
    fun validateAppGroup(group: AppGroup) {
        require(group.groupName.isNotBlank()) {
            "AppGroup.groupName must not be blank (id=${group.id})"
        }
    }

    /** @throws IllegalArgumentException if [zone] isn't safe to persist. */
    fun validateFocusZone(zone: FocusZone) {
        require(zone.name.isNotBlank()) {
            "FocusZone.name must not be blank (id=${zone.id})"
        }
        require(zone.radiusMeters > 0f) {
            "FocusZone.radiusMeters must be > 0, was ${zone.radiusMeters} (id=${zone.id})"
        }
        require(zone.latitude in -90.0..90.0) {
            "FocusZone.latitude must be between -90 and 90, was ${zone.latitude} (id=${zone.id})"
        }
        require(zone.longitude in -180.0..180.0) {
            "FocusZone.longitude must be between -180 and 180, was ${zone.longitude} (id=${zone.id})"
        }
    }
}
