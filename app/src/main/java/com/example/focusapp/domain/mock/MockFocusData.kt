package com.example.focusapp.domain.mock

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone

/**
 * MockFocusData
 * ----------------
 * Hand-written sample data for UI development and @Preview screens.
 * Deliberately plain Kotlin objects - no Room, no Context, no
 * FocusRepositoryProvider - so whoever is building a screen (Apps / Map /
 * Party Mode / History) can render real-looking data before a real screen
 * is wired up to FocusRepository.
 *
 * Safe to delete a given list once the screen it was standing in for
 * reads through FocusRepositoryProvider instead.
 *
 * See ui/screens/debug/DataLayerPreviewScreen.kt for a Compose screen
 * built entirely from this data - open that file in Android Studio and
 * look at its @Preview to see it rendered without running the app.
 */
object MockFocusData {

    /** Three sample zones, purely so DataLayerPreviewScreen has more than
     *  one row to show - the app itself only ever keeps one saved zone at
     *  a time (see FocusRepository.getFocusZone()). */
    val focusZones: List<FocusZone> = listOf(
        FocusZone(
            id = "zone-1",
            name = "Baillieu Library",
            latitude = -37.7963,
            longitude = 144.9614,
            radiusMeters = 60f
        ),
        FocusZone(
            id = "zone-2",
            name = "Home Study Desk",
            latitude = -37.8102,
            longitude = 144.9628,
            radiusMeters = 15f
        ),
        FocusZone(
            id = "zone-3",
            name = "ERC Level 2",
            latitude = -37.7969,
            longitude = 144.9605,
            radiusMeters = 40f
        )
    )

    val appGroups: List<AppGroup> = listOf(
        AppGroup(
            id = "group-1",
            groupName = "Social Media",
            packageNames = listOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.twitter.android"
            )
        ),
        AppGroup(
            id = "group-2",
            groupName = "Games",
            packageNames = listOf("com.supercell.clashofclans")
        )
    )

    val focusSessions: List<FocusSession> = listOf(
        FocusSession(
            id = "session-1",
            startTimeMillis = 1_726_000_000_000L,
            endTimeMillis = 1_726_003_600_000L,
            distractingAppOpenCount = 1,
            wasCompletedSuccessfully = true
        ),
        FocusSession(
            id = "session-2",
            startTimeMillis = 1_726_090_000_000L,
            endTimeMillis = 1_726_092_000_000L,
            distractingAppOpenCount = 5,
            wasCompletedSuccessfully = false
        ),
        FocusSession(
            id = "session-3",
            startTimeMillis = 1_726_170_000_000L,
            endTimeMillis = null,
            distractingAppOpenCount = 0,
            wasCompletedSuccessfully = false
        )
    )
}
