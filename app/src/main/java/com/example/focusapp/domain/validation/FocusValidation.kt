package com.example.focusapp.domain.validation

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusZone

/**
 * FocusValidation
 * ------------------
 * Field-level validation shared by every [com.example.focusapp.domain.repository.FocusRepository]
 * implementation. Deliberately plain Kotlin - no Room/Android import - so:
 *  - it runs as a fast JVM unit test with no emulator/Robolectric needed
 *  - it survives untouched if the local storage layer is ever swapped
 *    (Room -> SharedPreferences -> whatever else), matching the note
 *    already on [FocusZone] about not baking validation into the Entity.
 *
 * Deliberately does NOT check anything that needs a database round trip
 * (e.g. duplicate zone names, which FocusZone's own doc comment flags as
 * a TODO) - that kind of check needs a LocalDataSource lookup and belongs
 * in the Repository/UseCase layer right next to that call, not here.
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
