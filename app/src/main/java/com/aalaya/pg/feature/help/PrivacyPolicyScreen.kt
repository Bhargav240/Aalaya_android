package com.aalaya.pg.feature.help

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.theme.HamsaTheme

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    HamsaScreen(
        title = "Privacy policy",
        lead = "Your information stays protected and scoped to your property.",
        onBack = onBack,
    ) {
        item("privacy") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = HamsaTheme.colors.brand)
                Text("How we use your data", style = HamsaTheme.type.titleSmall, color = HamsaTheme.colors.textPrimary)
                Text(
                    "Aalaya uses account, stay, payment, and complaint information to provide the services you request. Access is limited to you and authorized staff in your property or branch. We do not display your private details to other residents.",
                    style = HamsaTheme.type.bodySmall,
                    color = HamsaTheme.colors.textSecondary,
                )
            }
        }
    }
}
