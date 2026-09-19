package com.example.focusapp.ui.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.domain.mock.MockFocusData
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone

/**
 * DataLayerPreviewScreen
 * --------------------------
 * NOT part of the app's real navigation graph - this screen exists purely
 * so the data layer has something visible with your own eyes, right in
 * Android Studio's @Preview pane, with no emulator, no Gradle build, and
 * no Firebase/Room actually running (it only ever reads [MockFocusData]).
 *
 * Open this file in Android Studio and look at the "DataLayerPreview"
 * preview in the split/design pane (or Build > "Deploy static previews"/
 * the little play icon above the @Preview function) to see it render.
 *
 * What it's demonstrating:
 *  - A LazyColumn rendering a list of sample FocusZones (the app itself
 *    only ever keeps one saved zone at a time - see FocusRepository.
 *    getFocusZone() - this list is just convenient for showing off the
 *    row layout with more than one card).
 *  - AppGroups with their package lists.
 *  - FocusSessions, including one still in-progress (endTimeMillis = null).
 *
 * Once a real screen reads through FocusRepositoryProvider instead of
 * MockFocusData, this preview screen can be deleted - it isn't wired into
 * NavGraph/Destinations on purpose, so deleting it never touches
 * navigation code.
 */
@Composable
fun DataLayerPreviewScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Focus Zones (${MockFocusData.focusZones.size})") }
        items(MockFocusData.focusZones) { zone -> FocusZoneCard(zone) }

        item { SectionTitle("App Groups (${MockFocusData.appGroups.size})") }
        items(MockFocusData.appGroups) { group -> AppGroupCard(group) }

        item { SectionTitle("Focus Sessions (${MockFocusData.focusSessions.size})") }
        items(MockFocusData.focusSessions) { session -> FocusSessionCard(session) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun FocusZoneCard(zone: FocusZone) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = zone.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "lat=${zone.latitude}, lng=${zone.longitude}, radius=${zone.radiusMeters}m",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AppGroupCard(group: AppGroup) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = group.groupName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = group.packageNames.joinToString(),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FocusSessionCard(session: FocusSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            val status = when {
                session.endTimeMillis == null -> "in progress"
                session.wasCompletedSuccessfully -> "completed"
                else -> "interrupted"
            }
            Text(text = "Session ${session.id} - $status", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "distracting app opens: ${session.distractingAppOpenCount}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true, name = "DataLayerPreview")
@Composable
private fun DataLayerPreviewScreenPreview() {
    MaterialTheme {
        Surface {
            DataLayerPreviewScreen()
        }
    }
}
