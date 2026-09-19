package com.example.focusapp.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_RADIUS_METERS = 50f
private const val MAX_RADIUS_METERS = 1000f
private const val DEFAULT_RADIUS_METERS = 150f

// 🎨 統一深色主題
private val ScreenBgColor = Color(0xFF33386C)       // 主背景色
private val CardBgColor = Color(0xFF252853)         // 卡片背景色
private val SelectedBgColor = Color(0xFF474E91)     // 按鈕與焦點色
private val AccentColor = Color(0xFF6C75CE)         // 強調色
private val TextPrimary = Color(0xFFFFFFFF)         // 主文字
private val TextSecondary = Color(0xFFA5ABC7)       // 次要文字

@Composable
fun EditLocationZoneScreen(
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionState = rememberLocationPermissionState()

    var isLoading by remember { mutableStateOf(true) }
    var existingZoneId by remember { mutableStateOf<String?>(null) }
    var zoneName by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableStateOf(DEFAULT_RADIUS_METERS) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val existing = withContext(Dispatchers.IO) {
            FocusRepositoryProvider.get(context).getFocusZone()
        }
        if (existing != null) {
            existingZoneId = existing.id
            zoneName = existing.name
            radiusMeters = existing.radiusMeters
            latitude = existing.latitude
            longitude = existing.longitude
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgColor)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // 1. 頂部列：左上角返回箭頭 + 標題
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    // The back arrow doubles as Save: persist the zone if a
                    // location's been captured, then always navigate back -
                    // there's no separate Save button anymore.
                    val lat = latitude
                    val lng = longitude
                    if (lat != null && lng != null) {
                        scope.launch {
                            val zone = FocusZone(
                                id = existingZoneId ?: "zone_${System.currentTimeMillis()}",
                                name = zoneName.ifBlank { "My Location" },
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = radiusMeters
                            )
                            withContext(Dispatchers.IO) {
                                FocusRepositoryProvider.get(context).saveFocusZone(zone)
                            }
                            onSaveComplete()
                        }
                    } else {
                        onSaveComplete()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardBgColor)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Edit Location Zone",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(24.dp))

        // 2. 輸入區塊 (可彈性佔據剩餘空間)
        Column(modifier = Modifier.weight(1f)) {
            // 區域名稱
            OutlinedTextField(
                value = zoneName,
                onValueChange = { zoneName = it },
                label = { Text("Zone name", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBgColor,
                    unfocusedContainerColor = CardBgColor,
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // 半徑調整卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBgColor,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Focus Radius",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${radiusMeters.toInt()} m",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = radiusMeters,
                        onValueChange = { radiusMeters = it },
                        valueRange = MIN_RADIUS_METERS..MAX_RADIUS_METERS,
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = AccentColor,
                            inactiveTrackColor = SelectedBgColor
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 定位卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBgColor,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        enabled = !isFetchingLocation,
                        onClick = {
                            if (!permissionState.hasPermission) {
                                permissionState.request()
                            } else {
                                isFetchingLocation = true
                                fetchCurrentLocation(context) { lat, lng ->
                                    latitude = lat
                                    longitude = lng
                                    isFetchingLocation = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SelectedBgColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                !permissionState.hasPermission -> "Allow location access"
                                isFetchingLocation -> "Locating…"
                                else -> "Use current location"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = when {
                            !permissionState.hasPermission ->
                                "Location permission not granted yet - tap the button above to allow it."
                            latitude != null && longitude != null -> "📍 Location set."
                            else -> "No location captured yet - tap \"Use current location\" to set one."
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    context: Context,
    onResult: (latitude: Double, longitude: Double) -> Unit
) {
    LocationServices.getFusedLocationProviderClient(context)
        .lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            }
        }
}

@Preview(showBackground = true)
@Composable
private fun EditLocationZoneScreenPreview() {
    EditLocationZoneScreen(onSaveComplete = {})
}