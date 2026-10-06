package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iocl.knowledgehub.ui.theme.*
import kotlin.math.*

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "dashboard"),
    BottomNavItem("Magazines", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "magazines"),
    BottomNavItem("Live News", Icons.Filled.Newspaper, Icons.Outlined.Newspaper, "live_news"),
    BottomNavItem("AI Chat", Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "chatbot"),
    BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person, "profile")
)

@Composable
fun IOCLBottomBar(
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bottom_bar")
    val glowAlpha by infiniteTransition.animateFloat(
        0.4f, 0.8f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.05f), // Subtle border
                    style = Stroke(width = 1.dp.toPx()),
                    cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
                )
            },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val animatedWeight by animateFloatAsState(
                    if (isSelected) 1.6f else 1f,
                    spring(dampingRatio = 0.7f, stiffness = 300f), label = "w"
                )
                val iconScale by animateFloatAsState(
                    when {
                        isPressed -> 0.85f
                        isSelected -> 1.1f
                        else -> 1f
                    },
                    spring(dampingRatio = 0.5f, stiffness = 400f), label = "scale"
                )
                val indicatorWidth by animateDpAsState(
                    if (isSelected) 24.dp else 0.dp,
                    spring(dampingRatio = 0.6f), label = "ind"
                )

                Box(
                    modifier = Modifier
                        .weight(animatedWeight)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (isSelected) Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            IOCLSaffron.copy(alpha = 0.12f),
                                            IOCLAmber.copy(alpha = 0.04f)
                                        )
                                    )
                                )
                                .drawBehind {
                                    // Glow under selected item
                                    drawRoundRect(
                                        color = IOCLSaffron.copy(alpha = glowAlpha * 0.1f),
                                        cornerRadius = CornerRadius(18.dp.toPx()),
                                        size = size
                                    )
                                }
                            else Modifier
                        )
                        .clickable(interactionSource, null) { onItemClick(item.route) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Icon(
                                if (isSelected) item.selectedIcon else item.unselectedIcon,
                                item.label,
                                Modifier.size(24.dp).scale(iconScale),
                                tint = if (isSelected) IOCLSaffron else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Animated indicator dot/line
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(indicatorWidth)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(
                                        listOf(IOCLSaffron, IOCLAmber)
                                    ) else Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                )
                        )

                        AnimatedVisibility(
                            isSelected,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Text(
                                item.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = IOCLSaffron,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
