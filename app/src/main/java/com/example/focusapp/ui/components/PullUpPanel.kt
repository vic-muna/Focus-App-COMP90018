package com.example.focusapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A fling faster than this (px/s) decides the direction regardless of how far the panel was dragged. */
private const val FLING_VELOCITY_THRESHOLD = 800f

/**
 * State for [PullUpPanel]. Besides the panel's own top edge, any other
 * element can start the drag via [pullUpDragHandle].
 */
@Stable
class PullUpPanelState internal constructor(
    initiallyExpanded: Boolean,
    private val scope: CoroutineScope,
) {
    var isExpanded by mutableStateOf(initiallyExpanded)
        private set

    /** 0 = fully expanded, 1 = fully collapsed (down to the panel's peek height). */
    internal var progress by mutableFloatStateOf(if (initiallyExpanded) 0f else 1f)
        private set

    /** How far (px) the panel travels between expanded and hidden - set by [PullUpPanel] on layout. */
    internal var travelPx = 0f

    private var animationJob: Job? = null

    internal val draggableState = DraggableState { delta -> dragBy(delta) }

    /** True while a scroll gesture inside the content is moving the panel (see [nestedScrollConnection]). */
    private var isNestedDragging = false

    /**
     * Lets scroll gestures inside the panel's content move the panel first,
     * like Google Maps' bottom sheet: swiping up raises the panel until it's
     * fully expanded, then scrolls the content; swiping down scrolls the
     * content back to its top, then lowers the panel. Taps are unaffected.
     */
    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.Drag || available.y >= 0f || progress <= 0f) return Offset.Zero
            return Offset(0f, startNestedDrag(available.y))
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.Drag || available.y <= 0f || progress >= 1f) return Offset.Zero
            return Offset(0f, startNestedDrag(available.y))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!isNestedDragging) return Velocity.Zero
            isNestedDragging = false
            settle(available.y)
            return available
        }
    }

    private fun startNestedDrag(deltaPx: Float): Float {
        if (!isNestedDragging) {
            isNestedDragging = true
            stopAnimation()
        }
        return dragBy(deltaPx)
    }

    /** Moves the panel by [deltaPx] (positive = down) and returns how much of it was used. */
    private fun dragBy(deltaPx: Float): Float {
        if (travelPx <= 0f) return 0f
        val old = progress
        progress = (progress + deltaPx / travelPx).coerceIn(0f, 1f)
        return (progress - old) * travelPx
    }

    fun expand() = settleTo(expanded = true)

    fun collapse() = settleTo(expanded = false)

    internal fun stopAnimation() {
        animationJob?.cancel()
    }

    internal fun settle(velocity: Float) {
        val expand = if (abs(velocity) > FLING_VELOCITY_THRESHOLD) velocity < 0f else progress < 0.5f
        settleTo(expand)
    }

    private fun settleTo(expanded: Boolean) {
        isExpanded = expanded
        animationJob?.cancel()
        animationJob = scope.launch {
            animate(initialValue = progress, targetValue = if (expanded) 0f else 1f) { value, _ ->
                progress = value
            }
        }
    }
}

@Composable
fun rememberPullUpPanelState(initiallyExpanded: Boolean = false): PullUpPanelState {
    val scope = rememberCoroutineScope()
    return remember { PullUpPanelState(initiallyExpanded, scope) }
}

/** Lets this element drag [state]'s panel up/down (swipe up to expand, down to collapse). */
fun Modifier.pullUpDragHandle(state: PullUpPanelState): Modifier = draggable(
    state = state.draggableState,
    orientation = Orientation.Vertical,
    onDragStarted = { state.stopAnimation() },
    onDragStopped = { velocity -> state.settle(velocity) },
)

/**
 * A panel docked to the bottom edge, over the screen's content, that the
 * user drags between two positions. Fill the screen with it (it lays itself
 * out at the bottom):
 *  - expanded: the panel's top sits at [expandedTopFraction] of the screen height
 *  - collapsed: only the top [peekHeight] of the panel stays on screen
 *    (0.dp hides it completely)
 * The panel follows drags on the strip along its top edge ([handleHeight]
 * tall) and scroll gestures inside [content] (see
 * [PullUpPanelState.nestedScrollConnection]) - so give [content] a scrollable
 * container (verticalScroll, LazyColumn...). Taps inside it never move the panel.
 * [collapseOnBack]: when true, the system back gesture collapses an expanded
 * panel before it navigates away - turn off for screens that open expanded.
 */
@Composable
fun PullUpPanel(
    state: PullUpPanelState,
    modifier: Modifier = Modifier,
    expandedTopFraction: Float = 0.32f,
    peekHeight: Dp = 0.dp,
    handleHeight: Dp = 24.dp,
    collapseOnBack: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(enabled = collapseOnBack && state.isExpanded) { state.collapse() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val panelHeight = maxHeight * (1f - expandedTopFraction)
        val travelPx = with(LocalDensity.current) {
            (panelHeight - peekHeight).coerceAtLeast(0.dp).toPx()
        }
        SideEffect { state.travelPx = travelPx }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(panelHeight)
                .offset { IntOffset(0, (state.progress * travelPx).roundToInt()) }
                .background(
                    FocusTheme.colors.background,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .nestedScroll(state.nestedScrollConnection),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(handleHeight)
                    .pullUpDragHandle(state)
            )
            content()
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun PullUpPanelCollapsedPreview() {
    FocusAppTheme {
        Box(Modifier.fillMaxSize().background(FocusTheme.colors.surface)) {
            PullUpPanel(state = rememberPullUpPanelState(), peekHeight = 200.dp) {
                Text(
                    text = "Panel content",
                    color = FocusTheme.colors.onSurface,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun PullUpPanelPreview() {
    FocusAppTheme {
        Box(Modifier.fillMaxSize().background(FocusTheme.colors.surface)) {
            PullUpPanel(state = rememberPullUpPanelState(initiallyExpanded = true)) {
                Text(
                    text = "Panel content",
                    color = FocusTheme.colors.onSurface,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}
