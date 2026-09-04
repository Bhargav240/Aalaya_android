package com.aalaya.pg.feature.explore

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.feature.enquiries.EnquiryRepository
import com.aalaya.pg.ui.components.HamsaButton
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.theme.HamsaTheme
import kotlinx.coroutines.launch

/**
 * The marketplace lead form for an authenticated explorer. Email OTP login
 * has already established the identity, so only the optional enquiry note is
 * required here.
 *
 * All network calls go through [EnquiryRepository] so failures surface the
 * backend's problem+json message (not a raw HTTP code) and 401s flow through
 * the shared authenticator.
 */
@Composable
fun EnquireSheet(
    listing: Listing,
    enquiries: EnquiryRepository,
) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Enquire about this PG",
            style = HamsaTheme.type.titleSmall,
            color = HamsaTheme.colors.textPrimary,
        )
        if (sent) {
            Spacer(Modifier.height(HamsaTheme.spacing.sm))
            Text(
                "Your enquiry was sent to the branch manager.",
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.brand,
            )
            return@HamsaCard
        }
        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        HamsaTextField(
            value = note,
            onValueChange = { note = it; error = null },
            label = "Tell them when you'd like to visit (optional)",
            enabled = !busy,
        )
        error?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.xxs))
            Text(it, style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.errorFg)
        }
        Spacer(Modifier.height(HamsaTheme.spacing.md))
        HamsaButton(
            text = if (busy) "Sending…" else "Send enquiry",
            onClick = {
                if (busy) return@HamsaButton
                busy = true
                scope.launch {
                    when (val r = enquiries.enquire(listing.branchId, note.takeIf { it.isNotBlank() })) {
                        is ApiResult.Success -> { sent = true; error = null }
                        is ApiResult.Failure -> error = r.error.message
                    }
                    busy = false
                }
            },
            enabled = !busy,
        )
    }
}
