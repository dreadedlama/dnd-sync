package `in`.dreadedlama.dndsync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.scrollAway
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ResponsiveTimeText
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.rememberColumnState
import com.google.android.horologist.compose.layout.scrollAway

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun Page(
    rotaryMode: ScalingLazyColumnState.RotaryMode = ScalingLazyColumnState.RotaryMode.Scroll,
    startTimeTextLinear: @Composable (() -> Unit)? = null,
    startTimeTextCurved: (CurvedScope.() -> Unit)? = null,
    content: ScalingLazyListScope.() -> Unit
) {
    val columnState = rememberColumnState(
        ScalingLazyColumnDefaults.responsive(
            rotaryMode = rotaryMode,
            hapticsEnabled = true,
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp, alignment = Alignment.Top
            )
        ),
    )

    Scaffold(timeText = {
        ResponsiveTimeText(
            modifier = Modifier.scrollAway(columnState),
            startCurvedContent = startTimeTextCurved,
            startLinearContent = startTimeTextLinear
        )
    }, positionIndicator = {
        PositionIndicator(columnState.state)
    }, vignette = {
        Vignette(vignettePosition = VignettePosition.TopAndBottom)
    }, modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        ScalingLazyColumn(
            columnState = columnState, content = content
        )
    }
}