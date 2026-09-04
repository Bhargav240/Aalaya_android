package com.aalaya.pg.feature.help

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.theme.HamsaTheme

@Composable
fun ContactUsScreen(onBack: () -> Unit) {
    HamsaScreen(
        title = "Contact us",
        lead = "We’re here to help with your stay.",
        onBack = onBack,
    ) {
        item("support") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                IconTile(
                    icon = Icons.AutoMirrored.Outlined.ContactSupport,
                    modifier = Modifier.padding(bottom = HamsaTheme.spacing.sm),
                )
                Text("Property support", style = HamsaTheme.type.titleSmall, color = HamsaTheme.colors.textPrimary)
                Text(
                    "For room, payment, complaint, or stay updates, contact your branch manager. They can resolve property-specific requests fastest.",
                    style = HamsaTheme.type.bodySmall,
                    color = HamsaTheme.colors.textSecondary,
                )
            }
        }
    }
}
