package com.iocl.knowledgehub.ui.chatbot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.domain.repository.ChatRepository
import com.iocl.knowledgehub.domain.usecase.chat.SendMessageUseCase
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val error: String? = null
)

// ═══════════════════════════════════════════════════════════════
// VIEWMODEL
// ═══════════════════════════════════════════════════════════════

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    val suggestions = listOf(
        "Latest IOCL magazine",
        "Safety guidelines",
        "HR policies overview",
        "Sustainability report",
        "Technical updates"
    )

    val quickStartItems = listOf(
        QuickStartItem(Icons.Outlined.MenuBook, "Magazines", "Browse latest issues", IOCLSaffron),
        QuickStartItem(Icons.Outlined.Newspaper, "Newspapers", "Daily news & updates", IOCLBlueAccent),
        QuickStartItem(Icons.Outlined.Security, "Safety", "Guidelines & protocols", IOCLGreen),
        QuickStartItem(Icons.Outlined.Policy, "Policies", "HR & admin policies", IOCLAmber),
        QuickStartItem(Icons.Outlined.Analytics, "Reports", "Analytics & insights", IOCLTeal),
        QuickStartItem(Icons.Outlined.Help, "Help", "Get assistance", IOCLCyan)
    )

    init {
        viewModelScope.launch {
            chatRepository.getChatHistory().collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(text: String? = null) {
        val messageText = (text ?: _uiState.value.inputText).trim()
        if (messageText.isBlank()) return

        _uiState.update {
            it.copy(
                inputText = "",
                isTyping = true,
                error = null
            )
        }

        viewModelScope.launch {
            when (val result = sendMessageUseCase(messageText)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isTyping = false
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isTyping = false,
                            error = result.message
                        )
                    }
                }

                is Resource.Loading -> {}
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _uiState.update { it.copy(messages = emptyList(), error = null) }
        }
    }
}

data class QuickStartItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val color: Color
)

// ═══════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    onBackClick: () -> Unit,
    viewModel: ChatbotViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // ── TTS Voice Manager ──
    val ttsManager = remember { ChatTtsManager(context) }
    val isTtsSpeaking by ttsManager.isSpeaking.collectAsState()

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    // Scroll-to-bottom FAB visibility
    val showScrollFab by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible < totalItems - 2
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        if (uiState.messages.isNotEmpty()) {
            val targetIndex = if (uiState.isTyping) uiState.messages.size else uiState.messages.size - 1
            listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle aurora background for the entire screen
        AuroraBackground(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.05f)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── PREMIUM HEADER ─────────────────────────────────
            PremiumChatHeader(
                isTyping = uiState.isTyping,
                onBackClick = onBackClick,
                onClearClick = viewModel::clearHistory
            )

            // ─── CHAT CONTENT ───────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty()) {
                    PremiumEmptyState(
                        quickStartItems = viewModel.quickStartItems,
                        suggestions = viewModel.suggestions,
                        onSuggestionClick = { viewModel.sendMessage(it) },
                        isTyping = uiState.isTyping
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { focusManager.clearFocus() }
                            },
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 12.dp, bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            uiState.messages,
                            key = { index, msg -> "chat_${index}_${msg.id}" }
                        ) { index, message ->
                            PremiumChatBubble(
                                message = message,
                                index = index,
                                onSuggestionClick = { viewModel.sendMessage(it) },
                                ttsManager = ttsManager,
                                isTtsSpeaking = isTtsSpeaking
                            )
                        }

                        // Typing indicator
                        if (uiState.isTyping) {
                            item(key = "typing_indicator") {
                                PremiumTypingIndicator()
                            }
                        }
                    }

                    // ─── SCROLL TO BOTTOM FAB ─────────────────────
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showScrollFab,
                            enter = scaleIn(animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            PremiumScrollFab(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(
                                            uiState.messages.size - 1
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ─── PREMIUM INPUT BAR ─────────────────────────────
            PremiumInputBar(
                text = uiState.inputText,
                onTextChange = viewModel::onInputChange,
                onSend = { viewModel.sendMessage() },
                isTyping = uiState.isTyping
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumChatHeader(
    isTyping: Boolean,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header")

    // Status dot pulse
    val statusScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "status_pulse"
    )
    val statusAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "status_alpha"
    )

    // Avatar glow
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "glow_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "glow_alpha"
    )

    // Gradient border rotation
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = LinearEasing)
        ), label = "border_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        IOCLNavyDark,
                        IOCLNavy,
                        IOCLNavy.copy(alpha = 0.95f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // Subtle geometric pattern
        GeometricPatternOverlay(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .alpha(0.15f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = IOCLWhite.copy(alpha = 0.9f)
                )
            }

            // AI Avatar with pulsing glow ring
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    IOCLSaffron.copy(alpha = glowAlpha),
                                    IOCLAmber.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Gradient border ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .drawBehind {
                            val rad = borderRotation * PI.toFloat() / 180f
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        IOCLSaffron,
                                        IOCLAmber,
                                        IOCLOrangeAccent,
                                        IOCLSaffronLight,
                                        IOCLSaffron
                                    ),
                                    center = Offset(
                                        size.width / 2 + cos(rad) * 4f,
                                        size.height / 2 + sin(rad) * 4f
                                    )
                                ),
                                radius = size.width / 2,
                                style = Stroke(width = 2.5f)
                            )
                        }
                )
                // Avatar icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(IOCLSaffron, IOCLOrangeAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = IOCLWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Title and status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IOCL AI Assistant",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    color = IOCLWhite,
                    style = MaterialTheme.typography.titleMedium.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(IOCLWhite, IOCLSaffronLight, IOCLWhite)
                        )
                    )
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated status dot
                    Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                        // Outer pulse
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(if (isTyping) 1f else statusScale)
                                .clip(CircleShape)
                                .background(
                                    (if (isTyping) IOCLSaffron else IOCLGreen)
                                        .copy(alpha = if (isTyping) 0.3f else statusAlpha * 0.3f)
                                )
                        )
                        // Core dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isTyping) IOCLSaffron else IOCLGreen)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isTyping) "Typing..." else "Online",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = if (isTyping) IOCLSaffronLight else IOCLGreenLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Clear chat button
            IconButton(onClick = onClearClick) {
                Icon(
                    Icons.Filled.DeleteSweep,
                    contentDescription = "Clear chat",
                    tint = IOCLWhite.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM CHAT BUBBLE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumChatBubble(
    message: ChatMessage,
    index: Int,
    onSuggestionClick: (String) -> Unit,
    ttsManager: ChatTtsManager? = null,
    isTtsSpeaking: Boolean = false
) {
    val isUser = message.isFromUser
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Staggered entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) {
        delay(60L * (index % 8).coerceAtMost(5))
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                if (isUser) slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) else slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                UserBubble(message = message)
            } else {
                AiBubble(
                    message = message,
                    ttsManager = ttsManager,
                    isTtsSpeaking = isTtsSpeaking
                )
            }

            // Timestamp
            Spacer(Modifier.height(3.dp))
            Text(
                text = sdf.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = IOCLGrayMedium.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Suggestion chips below AI messages
            if (!isUser && message.suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SuggestionChipRow(
                    suggestions = message.suggestions,
                    onClick = onSuggestionClick
                )
            }
        }
    }
}

// ─── User Bubble ────────────────────────────────────────────

@Composable
private fun UserBubble(message: ChatMessage) {
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = 20.dp, bottomEnd = 6.dp
    )

    Box(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(IOCLNavy, IOCLBlue, IOCLBlueSoft)
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = IOCLWhite,
            fontFamily = FontFamily.Serif,
            lineHeight = 20.sp
        )
    }
}

// ─── AI Bubble (Glassmorphism) ──────────────────────────────

@Composable
private fun AiBubble(
    message: ChatMessage,
    ttsManager: ChatTtsManager? = null,
    isTtsSpeaking: Boolean = false
) {
    val shape = RoundedCornerShape(
        topStart = 6.dp, topEnd = 20.dp,
        bottomStart = 20.dp, bottomEnd = 20.dp
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ai_bubble_${message.id}")
    val borderPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing)
        ), label = "ai_border"
    )

    val borderColor = if (message.isError) IOCLRed else IOCLSaffron
    val borderColor2 = if (message.isError) IOCLRedLight else IOCLBlueAccent

    Box(
        modifier = Modifier
            .widthIn(max = 310.dp)
            .clip(shape)
            .drawBehind {
                val rad = borderPhase * PI.toFloat() / 180f
                val sweepBrush = Brush.sweepGradient(
                    0f to borderColor.copy(alpha = 0.5f),
                    0.25f to borderColor2.copy(alpha = 0.25f),
                    0.5f to IOCLAmber.copy(alpha = 0.3f),
                    0.75f to borderColor.copy(alpha = 0.15f),
                    1f to borderColor.copy(alpha = 0.5f),
                    center = Offset(
                        size.width / 2 + cos(rad) * size.width * 0.1f,
                        size.height / 2 + sin(rad) * size.height * 0.1f
                    )
                )
                drawRoundRect(
                    brush = sweepBrush,
                    cornerRadius = CornerRadius(20f),
                    style = Stroke(width = 1.5f)
                )
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                shape = shape
            )
    ) {
        // Top reflection
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // AI label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(IOCLSaffron, IOCLAmber)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = IOCLWhite,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "IOCL AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = IOCLSaffron,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isError)
                    IOCLRed
                else
                    MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Serif,
                lineHeight = 21.sp
            )

            // ── Voice Playback Button ──
            if (!message.isError && ttsManager != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isTtsSpeaking)
                                IOCLSaffron.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        .clickable { ttsManager.toggle(message.content) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    CompactSpeakingDoodle(
                        isSpeaking = isTtsSpeaking,
                        size = 20.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isTtsSpeaking) "Stop" else "Listen",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTtsSpeaking) IOCLSaffron else IOCLGrayMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SUGGESTION CHIP ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SuggestionChipRow(
    suggestions: List<String>,
    onClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(suggestions) { index, suggestion ->
            var chipVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 100L)
                chipVisible = true
            }

            val scale by animateFloatAsState(
                targetValue = if (chipVisible) 1f else 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ), label = "chip_scale_$index"
            )
            val alpha by animateFloatAsState(
                targetValue = if (chipVisible) 1f else 0f,
                animationSpec = tween(300), label = "chip_alpha_$index"
            )

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.93f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "press_$index"
            )

            Box(
                modifier = Modifier
                    .scale(scale * pressScale)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(20.dp))
                    .drawBehind {
                        // Subtle border glow
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    IOCLSaffron.copy(alpha = 0.4f),
                                    IOCLAmber.copy(alpha = 0.2f)
                                )
                            ),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            style = Stroke(width = 1f)
                        )
                    }
                    .background(
                        color = IOCLSaffron.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = suggestion,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = IOCLSaffron
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM TYPING INDICATOR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_premium")

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        Row(
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small AI avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(IOCLSaffron.copy(alpha = 0.8f), IOCLAmber.copy(alpha = 0.6f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = IOCLWhite,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Glass pill with orbiting dots
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val dotOffset by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = -6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 500,
                                    delayMillis = index * 150,
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ), label = "dot_y_$index"
                        )
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.35f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 500,
                                    delayMillis = index * 150,
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ), label = "dot_a_$index"
                        )
                        val dotScale by infiniteTransition.animateFloat(
                            initialValue = 0.7f, targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 500,
                                    delayMillis = index * 150,
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ), label = "dot_s_$index"
                        )
                        Box(
                            modifier = Modifier
                                .offset(y = dotOffset.dp)
                                .scale(dotScale)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            IOCLSaffron.copy(alpha = dotAlpha),
                                            IOCLAmber.copy(alpha = dotAlpha * 0.5f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM EMPTY STATE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumEmptyState(
    quickStartItems: List<QuickStartItem>,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    isTyping: Boolean = false
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // ─── Animated AI Doodle ─────────────────────────────
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedAIDoodle(
                isSpeaking = false,
                isThinking = isTyping,
                size = 140.dp
            )
        }

        Spacer(Modifier.height(20.dp))

        // ─── Gradient title text ────────────────────────────
        Text(
            text = "Ask me anything",
            style = MaterialTheme.typography.headlineSmall.copy(
                brush = Brush.linearGradient(
                    colors = listOf(IOCLSaffron, IOCLAmber, IOCLSaffronLight)
                )
            ),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Your intelligent IOCL knowledge companion",
            style = MaterialTheme.typography.bodySmall,
            color = IOCLGrayMedium,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ─── Quick-start grid ───────────────────────────────
        Text(
            text = "QUICK START",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = IOCLGrayMedium,
            letterSpacing = 2.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // 2-column grid of quick start cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            quickStartItems.chunked(2).forEachIndexed { rowIndex, rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEachIndexed { colIndex, item ->
                        val cardIndex = rowIndex * 2 + colIndex
                        QuickStartCard(
                            item = item,
                            index = cardIndex,
                            modifier = Modifier.weight(1f),
                            onClick = { onSuggestionClick(item.title) }
                        )
                    }
                    // Fill remaining space if odd number of items
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ─── Suggestion chips ───────────────────────────────
        Text(
            text = "TRY ASKING",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = IOCLGrayMedium,
            letterSpacing = 2.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(suggestions) { index, suggestion ->
                val alpha = staggeredAlpha(index, baseDelay = 120)
                val offset = staggeredOffset(index, baseDelay = 120)

                Box(
                    modifier = Modifier
                        .alpha(alpha)
                        .offset(y = offset.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        IOCLSaffron.copy(alpha = 0.3f),
                                        IOCLAmber.copy(alpha = 0.15f)
                                    )
                                ),
                                cornerRadius = CornerRadius(24.dp.toPx()),
                                style = Stroke(width = 1f)
                            )
                        }
                        .background(
                            color = IOCLSaffron.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = IOCLSaffron.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = suggestion,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium,
                            color = IOCLSaffron
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Quick Start Card ───────────────────────────────────────

@Composable
private fun QuickStartCard(
    item: QuickStartItem,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha = staggeredAlpha(index, baseDelay = 100)
    val offset = staggeredOffset(index, baseDelay = 100)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_press_$index"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "card_$index")
    val borderPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(10000 + index * 1000, easing = LinearEasing)
        ), label = "card_border_$index"
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .offset(y = offset.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                val rad = borderPhase * PI.toFloat() / 180f
                drawRoundRect(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            item.color.copy(alpha = 0.3f),
                            item.color.copy(alpha = 0.1f),
                            item.color.copy(alpha = 0.2f),
                            item.color.copy(alpha = 0.05f),
                            item.color.copy(alpha = 0.3f)
                        ),
                        center = Offset(
                            size.width / 2 + cos(rad) * size.width * 0.1f,
                            size.height / 2 + sin(rad) * size.height * 0.1f
                        )
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1f)
                )
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = IOCLGrayMedium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM INPUT BAR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isTyping: Boolean
) {
    val focusManager = LocalFocusManager.current
    val isFocused = remember { mutableStateOf(false) }

    // Animated underline width
    val underlineWidth by animateFloatAsState(
        targetValue = if (isFocused.value) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ), label = "underline"
    )

    // Send button animation
    val sendEnabled = text.isNotBlank() && !isTyping
    val sendRotation by animateFloatAsState(
        targetValue = if (sendEnabled) 0f else 90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "send_rotation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val sendScale by animateFloatAsState(
        targetValue = if (isPressed && sendEnabled) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "send_scale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation = 2.dp
    ) {
        Column {
            // Animated saffron underline at top of input bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                // Background line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(IOCLGrayLight.copy(alpha = 0.3f))
                )
                // Animated gradient line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(underlineWidth)
                        .height(2.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    IOCLSaffron,
                                    IOCLAmber,
                                    IOCLSaffronLight
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Glass-style input field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = if (isFocused.value) listOf(
                                        IOCLSaffron.copy(alpha = 0.3f),
                                        IOCLAmber.copy(alpha = 0.15f)
                                    ) else listOf(
                                        IOCLGrayLight.copy(alpha = 0.4f),
                                        IOCLGrayLight.copy(alpha = 0.2f)
                                    )
                                ),
                                cornerRadius = CornerRadius(24.dp.toPx()),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    TextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        placeholder = {
                            Text(
                                "Ask something...",
                                color = IOCLGrayMedium.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = IOCLSaffron
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                focusManager.clearFocus()
                                isFocused.value = false
                                onSend()
                            }
                        ),
                        enabled = !isTyping,
                        interactionSource = remember { MutableInteractionSource() }.also { source ->
                            LaunchedEffect(source) {
                                source.interactions.collect { interaction ->
                                    when (interaction) {
                                        is androidx.compose.foundation.interaction.FocusInteraction.Focus -> {
                                            isFocused.value = true
                                        }
                                        is androidx.compose.foundation.interaction.FocusInteraction.Unfocus -> {
                                            isFocused.value = false
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Premium send button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(sendScale)
                        .clip(CircleShape)
                        .then(
                            if (sendEnabled) {
                                Modifier.background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(IOCLSaffron, IOCLAmber)
                                    )
                                )
                            } else {
                                Modifier.background(
                                    color = IOCLGrayLight.copy(alpha = 0.5f)
                                )
                            }
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = sendEnabled
                        ) {
                            focusManager.clearFocus()
                            isFocused.value = false
                            onSend()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (sendEnabled) IOCLWhite else IOCLGrayMedium,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = sendRotation
                            }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SCROLL TO BOTTOM FAB
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumScrollFab(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val fabGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "fab_glow"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .drawBehind {
                drawCircle(
                    color = IOCLSaffron.copy(alpha = fabGlow * 0.3f),
                    radius = size.width / 2 + 6.dp.toPx()
                )
            }
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(IOCLSaffron, IOCLAmber)
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "Scroll to bottom",
            tint = IOCLWhite,
            modifier = Modifier.size(24.dp)
        )
    }
}
