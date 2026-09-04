package com.aalaya.pg.feature.receipts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aalaya.pg.core.network.dto.ReceiptDto
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.theme.HamsaTheme

@Composable
fun ReceiptRow(
    receipt: ReceiptDto,
    downloading: Boolean,
    onDownload: () -> Unit,
    onOpen: () -> Unit = onDownload,
) {
    HamsaCard(padding = HamsaTheme.spacing.md, onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconTile(
                icon = Icons.Outlined.Description,
                size = 30.dp,
                iconSize = 16.dp,
                modifier = Modifier.padding(end = HamsaTheme.spacing.md),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Rent · ${DateFmt.epochSeconds(receipt.issuedEpoch).replace(Regex("^\\d{1,2} "), "")}", style = HamsaTheme.type.titleMedium, color = HamsaTheme.colors.textPrimary)
                Text(receipt.invoiceNumber ?: receipt.receiptNumber, style = HamsaTheme.type.labelMedium, color = HamsaTheme.colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (downloading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Outlined.Download, contentDescription = "Download receipt", tint = HamsaTheme.colors.brand)
                    }
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Open receipt", tint = HamsaTheme.colors.textTertiary)
            }
        }
    }
}
