package com.iocl.knowledgehub.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import com.iocl.knowledgehub.R
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.iocl.knowledgehub.domain.model.UserRole
import com.iocl.knowledgehub.ui.components.*
import com.iocl.knowledgehub.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

// ============================================================
// GOD-LEVEL LOGIN SCREEN — Google Sign-In
// Premium aurora-lit glassmorphism with one-tap Google login
// ============================================================

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    var isEmployeeMode by remember { mutableStateOf(true) }
    var adminUserId by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Success navigation & Permission Check ──
    LaunchedEffect(uiState.user) {
        val user = uiState.user
        if (user != null) {
            val intendedRole = if (isEmployeeMode) UserRole.EMPLOYEE else UserRole.ADMIN
            if (intendedRole == UserRole.ADMIN && user.role != UserRole.ADMIN && user.role != UserRole.SUPER_ADMIN) {
                // Deny access
                snackbarHostState.showSnackbar("Insufficient permissions. You are not an Admin.")
                viewModel.resetUser()
            } else {
                onLoginSuccess(intendedRole)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // ── Google Sign-In launcher ──
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    // No ID token — fallback to anonymous
                    viewModel.onGoogleSignInFailed()
                }
            } catch (e: Exception) {
                // Google Sign-In failed — auto fallback to anonymous
                viewModel.onGoogleSignInFailed()
            }
        } else {
            // User cancelled — auto fallback to anonymous
            viewModel.onGoogleSignInFailed()
        }
    }

    // ── Card entrance animation ──
    val cardScale = remember { Animatable(0.8f) }
    val cardTranslateY = remember { Animatable(with(density) { 50.dp.toPx() }) }
    val cardAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        kotlinx.coroutines.coroutineScope {
            launch { cardAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            launch {
                cardScale.animateTo(
                    1f, spring(dampingRatio = 0.65f, stiffness = 300f)
                )
            }
            launch {
                cardTranslateY.animateTo(
                    0f, spring(dampingRatio = 0.7f, stiffness = 250f)
                )
            }
        }
    }

    // ── Staggered entrance ──
    val staggerVisible = remember { mutableStateListOf(false, false, false, false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(700)
        for (i in staggerVisible.indices) {
            kotlinx.coroutines.delay(150)
            staggerVisible[i] = true
        }
    }

    // ── Decorative rings ──
    val infiniteTransition = rememberInfiniteTransition(label = "login_decor")
    val ringRotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(25000, easing = LinearEasing)),
        label = "ring_rot"
    )
    val ringPulse by infiniteTransition.animateFloat(
        0.9f, 1.1f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_pulse"
    )
    val ringPulse2 by infiniteTransition.animateFloat(
        1.05f, 0.92f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_pulse2"
    )

    // ── Gradient animation ──
    val gradientShift by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "btn_gradient"
    )
    val accentLineWidth by infiniteTransition.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "accent_line"
    )

    // ── Button press ──
    var isGooglePressed by remember { mutableStateOf(false) }
    val googleScale by animateFloatAsState(
        targetValue = if (isGooglePressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "google_scale"
    )
    var isGuestPressed by remember { mutableStateOf(false) }
    val guestScale by animateFloatAsState(
        targetValue = if (isGuestPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "guest_scale"
    )

    // ── Scroll parallax ──
    val scrollOffset = scrollState.value.toFloat()
    val parallaxBg = scrollOffset * 0.3f
    val parallaxParticles = scrollOffset * 0.15f
    val parallaxRings = scrollOffset * 0.2f

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E27),
                            Color(0xFF0D1333),
                            Color(0xFF111842),
                            Color(0xFF0A0E27)
                        )
                    )
                )
        ) {
        // ── Layer 1: Aurora Background ──
        AuroraBackground(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, -parallaxBg.toInt()) }
        )

        // ── Layer 2: Geometric Pattern ──
        GeometricPatternOverlay(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, -(parallaxBg * 0.5f).toInt()) }
        )

        // ── Layer 3: Floating Particles ──
        FloatingParticles(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, -parallaxParticles.toInt()) },
            particleCount = 45,
            colors = listOf(
                Color(0xFFFF9933).copy(alpha = 0.30f),
                Color(0xFFFFB347).copy(alpha = 0.22f),
                Color(0xFF4FC3F7).copy(alpha = 0.15f),
                Color(0xFFE8D5B7).copy(alpha = 0.10f),
                Color(0xFFFF6B35).copy(alpha = 0.18f),
                Color(0xFF7C4DFF).copy(alpha = 0.08f)
            )
        )

        // ── Main Content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // ── Logo Section with rings ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.offset { IntOffset(0, -parallaxRings.toInt()) }
            ) {
                DecorativeRing(
                    modifier = Modifier.size(180.dp),
                    rotation = ringRotation, scale = ringPulse,
                    color = IOCLSaffron.copy(alpha = 0.08f),
                    strokeWidth = 1.5f, dashPattern = true
                )
                DecorativeRing(
                    modifier = Modifier.size(150.dp),
                    rotation = -ringRotation * 0.7f, scale = ringPulse2,
                    color = IOCLBlueAccent.copy(alpha = 0.06f),
                    strokeWidth = 1f, dashPattern = false
                )
                DecorativeRing(
                    modifier = Modifier.size(120.dp),
                    rotation = ringRotation * 0.4f, scale = ringPulse,
                    color = IOCLAmber.copy(alpha = 0.05f),
                    strokeWidth = 0.8f, dashPattern = true
                )

                PulseGlow(color = IOCLSaffron, size = 100.dp)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_iocl_logo),
                        contentDescription = "IOCL Logo",
                        modifier = Modifier
                            .height(130.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "KNOWLEDGE HUB",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = IOCLSaffron,
                        letterSpacing = 8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // ── Glassmorphism Card ──
            GlassmorphismCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = cardScale.value
                        scaleY = cardScale.value
                        translationY = cardTranslateY.value
                        alpha = cardAlpha.value
                    },
                cornerRadius = 28.dp,
                blurRadius = 24.dp,
                borderWidth = 1.5.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Removed Role Selection Toggle ──

                    // ── Welcome Header ──
                    // ── Welcome Header ──
                    StaggeredElement(visible = staggerVisible.getOrElse(0) { false }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Welcome",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    brush = Brush.linearGradient(
                                        colors = listOf(IOCLWhite, IOCLWhite.copy(alpha = 0.85f))
                                    )
                                ),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sign in to access your knowledge hub",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IOCLGrayMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // ── Mode Selection Buttons ──
                    StaggeredElement(visible = staggerVisible.getOrElse(0) { false }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { isEmployeeMode = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEmployeeMode) IOCLSaffron else IOCLWhite.copy(alpha = 0.1f),
                                    contentColor = if (isEmployeeMode) IOCLWhite else IOCLWhite.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Employee", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            }
                            Button(
                                onClick = { isEmployeeMode = false },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isEmployeeMode) IOCLSaffron else IOCLWhite.copy(alpha = 0.1f),
                                    contentColor = if (!isEmployeeMode) IOCLWhite else IOCLWhite.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Admin", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // ══════════════════════════════════════════
                    // SIGN-IN UI
                    // ══════════════════════════════════════════
                    StaggeredElement(visible = staggerVisible.getOrElse(1) { false }) {
                        if (isEmployeeMode) {
                            GoogleSignInButton(
                                text = "Sign in as Employee",
                                isLoading = uiState.isLoading,
                                scale = googleScale,
                                onActionDown = { isGooglePressed = true },
                                onActionUp = { isGooglePressed = false },
                                onClick = {
                                    viewModel.setRole(UserRole.EMPLOYEE)
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken("722797832554-compute@developer.gserviceaccount.com")
                                        .requestEmail()
                                        .build()
                                    val client = GoogleSignIn.getClient(context, gso)
                                    client.signOut()
                                    googleSignInLauncher.launch(client.signInIntent)
                                }
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = adminUserId,
                                    onValueChange = { adminUserId = it },
                                    label = { Text("Admin User ID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = IOCLSaffron,
                                        unfocusedBorderColor = IOCLWhite.copy(alpha = 0.3f),
                                        focusedLabelColor = IOCLSaffron,
                                        unfocusedLabelColor = IOCLWhite.copy(alpha = 0.6f),
                                        focusedTextColor = IOCLWhite,
                                        unfocusedTextColor = IOCLWhite
                                    )
                                )
                                OutlinedTextField(
                                    value = adminPassword,
                                    onValueChange = { adminPassword = it },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = IOCLSaffron,
                                        unfocusedBorderColor = IOCLWhite.copy(alpha = 0.3f),
                                        focusedLabelColor = IOCLSaffron,
                                        unfocusedLabelColor = IOCLWhite.copy(alpha = 0.6f),
                                        focusedTextColor = IOCLWhite,
                                        unfocusedTextColor = IOCLWhite
                                    )
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = { viewModel.loginAsAdmin(adminUserId, adminPassword) },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IOCLBlueAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = IOCLWhite, strokeWidth = 2.dp)
                                    } else {
                                        Text("Login as Admin", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── OR divider ──
                    StaggeredElement(visible = staggerVisible.getOrElse(2) { false }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.5.dp)
                                    .background(IOCLGrayMedium.copy(alpha = 0.3f))
                            )
                            Text(
                                text = "  or  ",
                                style = MaterialTheme.typography.bodySmall,
                                color = IOCLGrayMedium.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Serif
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.5.dp)
                                    .background(IOCLGrayMedium.copy(alpha = 0.3f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ══════════════════════════════════════════
                    // CONTINUE AS GUEST BUTTON
                    // ══════════════════════════════════════════
                    StaggeredElement(visible = staggerVisible.getOrElse(3) { false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .graphicsLayer {
                                    scaleX = guestScale
                                    scaleY = guestScale
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            lerp(IOCLSaffron, IOCLSaffronDark, gradientShift * 0.3f),
                                            IOCLOrangeAccent,
                                            lerp(IOCLAmber, IOCLSaffronLight, gradientShift * 0.5f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .pointerInput(uiState.isLoading) {
                                    if (!uiState.isLoading) {
                                        detectTapGestures(
                                            onPress = {
                                                isGuestPressed = true
                                                tryAwaitRelease()
                                                isGuestPressed = false
                                            },
                                            onTap = { viewModel.signInAnonymously() }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.ExploreOff,
                                    contentDescription = null,
                                    tint = IOCLNavyDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue as Guest",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    color = IOCLNavyDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // ── Premium Footer ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(accentLineWidth)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    IOCLSaffron.copy(alpha = 0.4f),
                                    IOCLAmber.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Indian Oil Corporation Limited",
                    style = MaterialTheme.typography.bodySmall,
                    color = IOCLGrayMedium.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Energizing Lives Since 1959",
                    style = MaterialTheme.typography.labelSmall.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                IOCLSaffronLight.copy(alpha = 0.6f),
                                IOCLAmber.copy(alpha = 0.5f),
                                IOCLSaffronLight.copy(alpha = 0.6f)
                            )
                        ),
                        letterSpacing = 3.sp
                    ),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(IOCLSaffron.copy(alpha = 0.4f), CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© 2024 All Rights Reserved",
                    style = MaterialTheme.typography.labelSmall,
                    color = IOCLGray.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    } // End Scaffold
}

// ============================================================
// GOOGLE SIGN-IN BUTTON
// ============================================================
@Composable
fun GoogleSignInButton(
    text: String,
    isLoading: Boolean,
    scale: Float,
    onActionDown: suspend () -> Unit,
    onActionUp: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .pointerInput(isLoading) {
                if (!isLoading) {
                    detectTapGestures(
                        onPress = {
                            onActionDown()
                            tryAwaitRelease()
                            onActionUp()
                        },
                        onTap = { onClick() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF4285F4),
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "G",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF1F1F1F)
                )
            }
        }
    }
}

// ============================================================
// DECORATIVE RING — Rotating dashed/solid circle
// ============================================================
@Composable
fun DecorativeRing(
    modifier: Modifier = Modifier,
    rotation: Float,
    scale: Float,
    color: Color,
    strokeWidth: Float,
    dashPattern: Boolean
) {
    Canvas(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
    ) {
        val radius = size.minDimension / 2
        if (dashPattern) {
            val pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(12f, 8f), 0f
            )
            drawCircle(
                color = color,
                radius = radius,
                style = Stroke(width = strokeWidth.dp.toPx(), pathEffect = pathEffect)
            )
        } else {
            drawCircle(
                color = color,
                radius = radius,
                style = Stroke(width = strokeWidth.dp.toPx())
            )
        }
    }
}

// ============================================================
// STAGGERED ELEMENT — Entrance animation wrapper
// ============================================================
@Composable
fun StaggeredElement(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 350f
                    )
                ),
        exit = fadeOut(tween(200))
    ) {
        content()
    }
}

// ============================================================
// ORBITING DOTS LOADER
// ============================================================
@Composable
fun OrbitingDotsLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "orbiting")
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "orbit_rot"
    )
    Canvas(modifier = Modifier.size(32.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 3
        for (i in 0..2) {
            val angle = (rotation + i * 120f) * PI.toFloat() / 180f
            val dotX = center.x + cos(angle) * radius
            val dotY = center.y + sin(angle) * radius
            val alpha = 1f - (i * 0.3f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = (4 - i).dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }
    }
}
