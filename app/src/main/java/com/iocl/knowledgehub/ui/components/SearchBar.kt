package com.iocl.knowledgehub.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import com.iocl.knowledgehub.ui.theme.*

@Composable
fun IOCLSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String = "Search documents...",
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderAlpha by animateFloatAsState(
        if (isFocused) 0.6f else 0f,
        tween(400), label = "border"
    )
    val elevation by animateDpAsState(
        if (isFocused) 16.dp else 8.dp,
        tween(400), label = "elev"
    )
    val searchIconRotation by animateFloatAsState(
        if (query.isNotEmpty()) 0f else -15f,
        spring(dampingRatio = 0.5f), label = "rot"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(18.dp))
            .drawBehind {
                if (borderAlpha > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                IOCLSaffron.copy(alpha = borderAlpha),
                                IOCLAmber.copy(alpha = borderAlpha * 0.5f),
                                IOCLSaffron.copy(alpha = borderAlpha)
                            )
                        ),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = Stroke(2f)
                    )
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search, "Search",
                    tint = if (isFocused) IOCLSaffron else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = searchIconRotation }
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    query.isNotEmpty(),
                    enter = fadeIn() + scaleIn(spring(dampingRatio = 0.5f)),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Filled.Clear, "Clear",
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = IOCLSaffron
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            singleLine = true
        )
    }
}
