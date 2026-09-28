package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

// The building blocks every card in the app shares:
//
//   FocusCard {                          <- rounded card
//       CardButtonRow(left, right)       <- e.g. X on the left, check on the right
//       CardTitle("Name")                <- big title
//       CardLabel("Connected now")       <- small grey label
//       Box(Modifier.sunkenPanel(...))   <- darker rounded area inside the card
//   }

/** The rounded card background. Put the card's content inside. */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FocusTheme.colors.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content,
    )
}

/** The card's top row: one button on the left, one on the right. */
@Composable
fun CardButtonRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        left()
        right()
    }
}

/** A card's title, under the button row. */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = FocusTheme.typography.tileTitle,
        color = FocusTheme.colors.onSurface,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** A section heading inside a summary card (e.g. "Blocked Apps"). */
@Composable
fun CardSectionTitle(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.tileTitle,
        color = FocusTheme.colors.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
    )
}

/** A small grey label above a part of a card. */
@Composable
fun CardLabel(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.caption,
        color = FocusTheme.colors.onSurfaceMuted,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
    )
}

/** A darker, rounded area inside a card. Pass `FocusTheme.colors.surfaceSunken`. */
fun Modifier.sunkenPanel(color: Color): Modifier = this
    .fillMaxWidth()
    .clip(RoundedCornerShape(12.dp))
    .background(color)

@Preview(widthDp = 360)
@Composable
private fun FocusCardPreview() {
    FocusAppTheme {
        FocusCard(modifier = Modifier.padding(16.dp)) {
            CardButtonRow(
                left = { RejectButton(onClick = {}) },
                right = { ConfirmButton(onClick = {}) },
            )
            CardTitle("Card title")
            CardLabel("Label")
            Text(
                text = "Inside a sunken panel",
                style = FocusTheme.typography.body,
                color = FocusTheme.colors.onSurface,
                modifier = Modifier
                    .sunkenPanel(FocusTheme.colors.surfaceSunken)
                    .padding(16.dp),
            )
            CardSectionTitle("Section title")
        }
    }
}
