package com.aalaya.pg.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.aalaya.pg.ui.theme.HamsaTheme

/** Blur radius behind every modal surface. One value, so overlays match. */
val ModalBlurRadius: Dp = 28.dp

/** Scrim opacity behind every modal surface, used when blur is unavailable. */
const val ModalDimAmount: Float = 0.45f

/**
 * Dims and blurs whatever sits behind a modal window.
 *
 * Call this once from inside any `Dialog` content — [HamsaSheet] and the
 * date pickers already do — and the effect is identical everywhere. It reaches
 * the dialog's own window through [DialogWindowProvider] and works on the
 * window itself, so it adds no layout and cannot intercept touches: the popup
 * stays fully interactive and is never blurred by its own backdrop.
 *
 * Dim is applied unconditionally because blur is not always available: it needs
 * API 31+, and the system still switches it off on low-end devices, in battery
 * saver, and on most emulators (`isCrossWindowBlurEnabled`). Relying on blur
 * alone would leave the background untouched exactly where testing happens.
 * The radius is reset on dispose so it cannot leak into a later dialog reusing
 * the same window.
 */
@Composable
fun ModalBackdrop(
    blurRadius: Dp = ModalBlurRadius,
    dimAmount: Float = ModalDimAmount,
) {
    val view = LocalView.current
    val radiusPx = with(LocalDensity.current) { blurRadius.roundToPx() }

    DisposableEffect(view, radiusPx, dimAmount) {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setDimAmount(dimAmount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window?.setBackgroundBlurRadius(radiusPx)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window?.setBackgroundBlurRadius(0)
            }
        }
    }
}

/**
 * Material's [AlertDialog] with the app's backdrop and surface colour applied.
 *
 * Use this instead of [AlertDialog] directly so confirmations dim and blur the
 * same way sheets do. The backdrop rides along in the `confirmButton` slot —
 * that slot is always present, and [ModalBackdrop] emits no layout of its own,
 * so the button row is unaffected.
 */
@Composable
fun HamsaAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ModalBackdrop()
            confirmButton()
        },
        modifier = modifier,
        dismissButton = dismissButton,
        title = title,
        text = text,
        containerColor = HamsaTheme.colors.surface,
        titleContentColor = HamsaTheme.colors.textPrimary,
        textContentColor = HamsaTheme.colors.textSecondary,
    )
}
