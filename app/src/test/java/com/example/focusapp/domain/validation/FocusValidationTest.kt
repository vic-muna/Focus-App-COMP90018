package com.example.focusapp.domain.validation

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusZone
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * FocusValidationTest
 * -----------------------
 * Plain local unit test (host JVM, no emulator) - FocusValidation has no
 * Android/Room import, so this is as fast/simple as ExampleUnitTest.
 */
class FocusValidationTest {

    @Test
    fun validateAppGroup_acceptsNonBlankName() {
        FocusValidation.validateAppGroup(AppGroup(id = "g1", groupName = "Social Media"))
        // no exception thrown = pass
    }

    @Test
    fun validateAppGroup_rejectsBlankName() {
        assertThrows(IllegalArgumentException::class.java) {
            FocusValidation.validateAppGroup(AppGroup(id = "g1", groupName = "   "))
        }
    }

    @Test
    fun validateFocusZone_acceptsValidZone() {
        FocusValidation.validateFocusZone(
            FocusZone(id = "z1", name = "Library", latitude = -37.8, longitude = 144.9, radiusMeters = 50f)
        )
    }

    @Test
    fun validateFocusZone_rejectsBlankName() {
        assertThrows(IllegalArgumentException::class.java) {
            FocusValidation.validateFocusZone(
                FocusZone(id = "z1", name = "", latitude = -37.8, longitude = 144.9, radiusMeters = 50f)
            )
        }
    }

    @Test
    fun validateFocusZone_rejectsZeroRadius() {
        assertThrows(IllegalArgumentException::class.java) {
            FocusValidation.validateFocusZone(
                FocusZone(id = "z1", name = "Library", latitude = -37.8, longitude = 144.9, radiusMeters = 0f)
            )
        }
    }

    @Test
    fun validateFocusZone_rejectsNegativeRadius() {
        assertThrows(IllegalArgumentException::class.java) {
            FocusValidation.validateFocusZone(
                FocusZone(id = "z1", name = "Library", latitude = -37.8, longitude = 144.9, radiusMeters = -5f)
            )
        }
    }

    @Test
    fun validateFocusZone_rejectsOutOfRangeLatitude() {
        assertThrows(IllegalArgumentException::class.java) {
            FocusValidation.validateFocusZone(
                FocusZone(id = "z1", name = "Library", latitude = 137.0, longitude = 144.9, radiusMeters = 50f)
            )
        }
    }

    @Test
    fun validateFocusZone_rejectsOutOfRangeLongitude() {
        assertThrows(IllegalArgumentException::class.java) {
            FocusValidation.validateFocusZone(
                FocusZone(id = "z1", name = "Library", latitude = -37.8, longitude = 244.9, radiusMeters = 50f)
            )
        }
    }
}
