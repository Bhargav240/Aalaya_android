package com.aalaya.pg.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aalaya.pg.core.data.PinStore
import com.aalaya.pg.ui.theme.HamsaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PinSetupStage {
    CREATE,
    CONFIRM,
}

/**
 * Clean, modern, and user-friendly App PIN setup & unlock screen.
 * Supports two-step verification during setup (Create -> Confirm) before saving.
 */
@Composable
fun PinScreen(
    pinStore: PinStore,
    title: String,
    onUnlocked: () -> Unit,
    /** When set (first-time setup), a fresh 4-digit entry is created and confirmed before saving. */
    onSubmitPin: ((String) -> Unit)? = null,
) {
    val colors = HamsaTheme.colors
    val scope = rememberCoroutineScope()
    val isSetup = onSubmitPin != null

    var stage by remember { mutableStateOf(PinSetupStage.CREATE) }
    var createPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var unlockEntry by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val currentEntry = when {
        !isSetup -> unlockEntry
        stage == PinSetupStage.CREATE -> createPin
        else -> confirmPin
    }

    fun handleDigit(char: Char) {
        if (isSuccess) return
        error = null

        if (!isSetup) {
            if (unlockEntry.length < 4) {
                val updated = unlockEntry + char
                unlockEntry = updated
                if (updated.length == 4) {
                    scope.launch {
                        delay(120)
                        val ok = pinStore.checkPin(updated)
                        if (ok) {
                            isSuccess = true
                            delay(150)
                            onUnlocked()
                        } else {
                            error = "Incorrect PIN. Please try again."
                            unlockEntry = ""
                        }
                    }
                }
            }
            return
        }

        // Setup flow
        if (stage == PinSetupStage.CREATE) {
            if (createPin.length < 4) {
                val updated = createPin + char
                createPin = updated
                if (updated.length == 4) {
                    scope.launch {
                        delay(200)
                        stage = PinSetupStage.CONFIRM
                        confirmPin = ""
                        error = null
                    }
                }
            }
        } else {
            // CONFIRM stage
            if (confirmPin.length < 4) {
                val updated = confirmPin + char
                confirmPin = updated
                if (updated.length == 4) {
                    scope.launch {
                        delay(150)
                        if (updated == createPin) {
                            isSuccess = true
                            error = null
                            delay(200)
                            onSubmitPin?.invoke(createPin)
                            onUnlocked()
                        } else {
                            error = "PINs do not match. Please try again."
                            confirmPin = ""
                        }
                    }
                }
            }
        }
    }

    fun handleDelete() {
        if (isSuccess) return
        error = null
        if (!isSetup) {
            unlockEntry = unlockEntry.dropLast(1)
        } else if (stage == PinSetupStage.CREATE) {
            createPin = createPin.dropLast(1)
        } else {
            confirmPin = confirmPin.dropLast(1)
        }
    }

    fun handleReset() {
        if (isSuccess) return
        stage = PinSetupStage.CREATE
        createPin = ""
        confirmPin = ""
        error = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top Section: Icon, Title, Subtitle, Step Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.height(16.dp))

            // Security Icon Container
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.brandSubtle, CircleShape)
                    .border(1.dp, colors.brandSubtle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        isSuccess -> Icons.Outlined.CheckCircle
                        isSetup && stage == PinSetupStage.CONFIRM -> Icons.Outlined.VerifiedUser
                        else -> Icons.Outlined.Lock
                    },
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Title & Subtitle with smooth transition
            AnimatedContent(
                targetState = if (isSetup) stage else null,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "PinTitle",
            ) { currentStage ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            !isSetup -> title.ifBlank { "Enter App PIN" }
                            currentStage == PinSetupStage.CREATE -> "Create App PIN"
                            else -> "Confirm your PIN"
                        },
                        style = HamsaTheme.type.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = when {
                            !isSetup -> "Enter your 4-digit PIN to unlock Aalaya"
                            currentStage == PinSetupStage.CREATE -> "Set a 4-digit PIN to protect staff actions and quick unlock"
                            else -> "Re-enter the 4-digit PIN to confirm"
                        },
                        style = HamsaTheme.type.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Step Badge during setup
            if (isSetup) {
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(colors.surface, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (stage == PinSetupStage.CREATE) colors.brand else colors.borderStrong,
                                CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (stage == PinSetupStage.CONFIRM) colors.brand else colors.borderStrong,
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = if (stage == PinSetupStage.CREATE) "Step 1 of 2: Create" else "Step 2 of 2: Confirm",
                        style = HamsaTheme.type.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.brand,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // PIN Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(4) { index ->
                    val isFilled = index < currentEntry.length
                    val hasError = error != null

                    val dotBorderColor = when {
                        hasError -> colors.errorFg
                        isSuccess -> colors.brand
                        isFilled -> colors.brand
                        else -> colors.borderStrong
                    }

                    val dotBgColor = when {
                        hasError -> colors.errorFg
                        isSuccess -> colors.brand
                        isFilled -> colors.brand
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(dotBgColor)
                            .border(2.dp, dotBorderColor, CircleShape),
                    )
                }
            }

            // Error Message
            AnimatedVisibility(visible = error != null) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = colors.errorFg,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = error ?: "",
                        style = HamsaTheme.type.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.errorFg,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Option to start over during confirmation
            if (isSetup && stage == PinSetupStage.CONFIRM) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { handleReset() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Start over",
                        tint = colors.brand,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Change initial PIN",
                        style = HamsaTheme.type.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.brand,
                    )
                }
            }
        }

        // Bottom Section: Numeric Keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            PinKeypad(
                onDigit = ::handleDigit,
                onDelete = ::handleDelete,
                onReset = if (isSetup && stage == PinSetupStage.CONFIRM) ::handleReset else null,
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onReset: (() -> Unit)?,
) {
    val colors = HamsaTheme.colors
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('R', '0', '⌫'),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { cell ->
                    when (cell) {
                        'R' -> {
                            if (onReset != null) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable { onReset() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Reset",
                                        style = HamsaTheme.type.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.brand,
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(72.dp))
                            }
                        }
                        '⌫' -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, CircleShape)
                                    .clickable { onDelete() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Delete",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, CircleShape)
                                    .clickable { onDigit(cell) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = cell.toString(),
                                    style = HamsaTheme.type.headlineMedium.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
