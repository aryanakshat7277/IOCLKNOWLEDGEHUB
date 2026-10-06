package com.iocl.knowledgehub.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.AuthRepository
import com.iocl.knowledgehub.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSent: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun resetPassword() {
        if (_uiState.value.email.isBlank() || !_uiState.value.email.contains("@")) {
            _uiState.update { it.copy(error = "Please enter a valid email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.resetPassword(_uiState.value.email.trim())) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSent = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cardAlpha = remember { Animatable(0f) }
    val cardOffset = remember { Animatable(40f) }
    LaunchedEffect(Unit) {
        cardAlpha.animateTo(1f, tween(600, 200))
    }
    LaunchedEffect(Unit) {
        cardOffset.animateTo(0f, tween(600, 200, FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(IOCLNavyDark, IOCLNavy, IOCLBlue)
                )
            )
    ) {
        // Subtle radial glow accent
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(IOCLSaffron.copy(alpha = 0.06f), Color.Transparent),
                        radius = 400f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = IOCLWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Animated icon
            Icon(
                Icons.Filled.LockReset,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .alpha(cardAlpha.value),
                tint = IOCLSaffron
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = IOCLWhite,
                modifier = Modifier.alpha(cardAlpha.value)
            )

            Text(
                text = "Enter your email address and we'll send you\na link to reset your password",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Serif,
                color = IOCLGrayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .alpha(cardAlpha.value)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Content area with entrance animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardAlpha.value)
                    .offset(y = cardOffset.value.dp)
            ) {
                if (uiState.isSent) {
                    // Success State
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = IOCLGreen.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = IOCLGreenLight.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = IOCLGreenLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Email Sent!",
                                style = MaterialTheme.typography.titleLarge,
                                color = IOCLGreenLight,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "Check your inbox for the password reset link",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Serif,
                                color = IOCLGrayMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = onBackClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = IOCLSaffronLight
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = IOCLSaffronLight.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = "Return to Login",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Input State
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            label = { Text("Employee Email") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = IOCLSaffronLight
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IOCLSaffron,
                                unfocusedBorderColor = GlassBorder,
                                focusedLabelColor = IOCLSaffronLight,
                                unfocusedLabelColor = IOCLGrayMedium,
                                cursorColor = IOCLSaffron,
                                focusedTextColor = IOCLWhite,
                                unfocusedTextColor = IOCLWhite
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )

                        // Error
                        AnimatedVisibility(
                            visible = uiState.error != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = IOCLRed.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = IOCLRedLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.error ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IOCLRedLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = viewModel::resetPassword,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            enabled = !uiState.isLoading
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(IOCLSaffron, IOCLAmber)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = IOCLWhite,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Send Reset Link",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IOCLNavyDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.isSent) {
                TextButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = IOCLSaffronLight
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Back to Login",
                        color = IOCLSaffronLight,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Text(
                text = "Indian Oil Corporation Limited",
                style = MaterialTheme.typography.bodySmall,
                color = IOCLGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
