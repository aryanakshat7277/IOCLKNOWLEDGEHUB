package com.iocl.knowledgehub.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val IOCLShapes = Shapes(
    // Buttons, small chips
    extraSmall = RoundedCornerShape(8.dp),
    // Input fields, small cards
    small = RoundedCornerShape(12.dp),
    // Standard cards, dialogs
    medium = RoundedCornerShape(16.dp),
    // Large cards, bottom sheets
    large = RoundedCornerShape(24.dp),
    // Full-screen modals, hero sections
    extraLarge = RoundedCornerShape(32.dp)
)
