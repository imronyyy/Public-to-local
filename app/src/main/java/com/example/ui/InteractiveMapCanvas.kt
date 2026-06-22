package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Business
import com.example.ui.theme.*
import android.location.Location
import kotlin.math.max

@Composable
fun InteractiveMapCanvas(
    userLocation: Location?,
    businesses: List<Business>,
    selectedBusiness: Business?,
    onBusinessSelected: (Business?) -> Unit,
    getDistanceText: (Business) -> String
) {
    val isSystemDark = isSystemInDarkTheme()
    val mapBgColor = if (isSystemDark) Color(0xFF0B0F19) else Color(0xFFF1F5F9)
    val gridLineColor = if (isSystemDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .testTag("interactive_map_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(mapBgColor)
                    .clickable {
                        if (businesses.isNotEmpty()) {
                            val activeIndex = when {
                                selectedBusiness == null -> 0
                                else -> (businesses.indexOfFirst { it.id == selectedBusiness.id } + 1) % businesses.size
                            }
                            onBusinessSelected(businesses.getOrNull(activeIndex))
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f

                // Draw neighborhood local grids
                val numGrid = 8
                val stepX = width / numGrid
                val stepY = height / numGrid
                for (i in 0..numGrid) {
                    drawLine(gridLineColor, Offset(i * stepX, 0f), Offset(i * stepX, height), 1.5f)
                    drawLine(gridLineColor, Offset(0f, i * stepY), Offset(width, i * stepY), 1.5f)
                }
                
                // Street bypass avenue road
                drawLine(
                    color = if (isSystemDark) Color(0xFF1E293B) else Color(0xFFCBD5E1),
                    start = Offset(0f, height * 0.15f),
                    end = Offset(width, height * 0.85f),
                    strokeWidth = 24f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(0f, height * 0.15f),
                    end = Offset(width, height * 0.85f),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )

                // Pulsating User node
                drawCircle(
                    color = BlueInfo.copy(alpha = 0.2f),
                    radius = 35f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = BlueInfo,
                    radius = 8f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(centerX, centerY)
                )

                // Draw partner coordinates mapping
                businesses.forEach { biz ->
                    val scaleX = 25000f
                    val scaleY = 25000f
                    val baseLat = 28.6139
                    val baseLng = 77.2090
                    
                    val offsetLng = (biz.longitude - baseLng).toFloat() * scaleX
                    val offsetLat = (baseLat - biz.latitude).toFloat() * scaleY
                    
                    val bX = centerX + offsetLng
                    val bY = centerY + offsetLat
                    
                    val finalX = bX.coerceIn(24f, width - 24f)
                    val finalY = bY.coerceIn(24f, height - 24f)

                    val nodeColor = when(biz.category.lowercase()) {
                        "health" -> RedEmergency
                        "education" -> BlueInfo
                        "food/lifestyle" -> TealTertiary
                        "emergency" -> RedEmergency
                        else -> TealSecondary
                    }

                    val isNavTarget = selectedBusiness?.id == biz.id
                    if (isNavTarget) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.35f),
                            radius = 28f,
                            center = Offset(finalX, finalY)
                        )
                        drawLine(
                            color = TealSecondary,
                            start = Offset(centerX, centerY),
                            end = Offset(finalX, finalY),
                            strokeWidth = 3.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    drawCircle(
                        color = nodeColor,
                        radius = 6f,
                        center = Offset(finalX, finalY)
                    )
                }
            }

            // Route details HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (selectedBusiness != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TealSecondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Route to ${selectedBusiness.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row {
                                        Text(
                                            "Dist: ${getDistanceText(selectedBusiness)}",
                                            fontSize = 10.sp,
                                            color = TealSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val walkingMin = max(1, (selectedBusiness.reviewsCount % 12) + 3)
                                        Text(
                                            "⏳ Walk: $walkingMin mins",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Active Routing Walk",
                                tint = TealSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BlueInfo)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Tap any Service Provider card or directory node below to route.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "My GPS Status",
                                tint = BlueInfo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
