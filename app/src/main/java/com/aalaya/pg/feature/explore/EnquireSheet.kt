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
import androidx.compose.ui.text.input.KeyboardType
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.feature.enquiries.EnquiryRepository
import com.aalaya.pg.ui.components.HamsaButton
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.theme.HamsaTheme
import kotlinx.coroutines.launch

/**
 * The marketplace lead form for an authenticated explorer.
 * Collects contact phone number and visit note so the property manager can reach out.
 */
@Composable
fun EnquireSheet(
    listing: Listing,
    enquiries: EnquiryRepository,
) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
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
                "Your enquiry was sent to the branch manager. They will reach out to you shortly.",
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.brand,
            )
            return@HamsaCard
        }
        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        HamsaTextField(
            value = phone,
            onValueChange = { phone = it; error = null },
            label = "Phone number (for manager to call/WhatsApp)",
            placeholder = "10-digit mobile number",
            keyboardType = KeyboardType.Phone,
            enabled = !busy,
        )
        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        HamsaTextField(
            value = note,
            onValueChange = { note = it; error = null },
            label = "Tell them when you'd like to visit (optional)",
            placeholder = "e.g. Visiting this Saturday afternoon",
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
                val cleanPhone = phone.trim()
                if (cleanPhone.isNotEmpty() && cleanPhone.filter { it.isDigit() }.length < 10) {
                    error = "Please enter a valid 10-digit phone number"
                    return@HamsaButton
                }
                busy = true
                scope.launch {
                    when (val r = enquiries.enquire(
                        branchId = listing.branchId,
                        note = note.takeIf { it.isNotBlank() },
                        phone = cleanPhone.takeIf { it.isNotBlank() },
                    )) {
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
