package com.aalaya.pg.feature.away

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.DateField
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.ListRow
import com.aalaya.pg.ui.components.ModalBackdrop
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.theme.HamsaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/** Which field the open date picker is editing. */
private enum class Picking { From, To }

/**
 * "Report away days" — the resident tells the PG when their bed will be empty
 * so nobody worries. A safety check-in, not a permission request.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwayScreen(viewModel: AwayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors
    var picking by remember { mutableStateOf<Picking?>(null) }

    HamsaScreen(
        title = "Report away days",
        lead = "A safety feature — the PG knows not to worry when your bed is empty.",
    ) {
        item("form") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                DateField(
                    label = "Away from",
                    value = state.from?.format(dayFormat),
                    placeholder = "Pick a date",
                    onClick = { picking = Picking.From },
                )
                Spacer(Modifier.height(HamsaTheme.spacing.stack))
                DateField(
                    label = "Back on",
                    value = state.to?.format(dayFormat),
                    placeholder = "Pick a date",
                    onClick = { picking = Picking.To },
                )
                state.error?.let {
                    Text(
                        it,
                        style = HamsaTheme.type.labelMedium,
                        color = colors.errorFg,
                        modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
                    )
                }
                Spacer(Modifier.height(HamsaTheme.spacing.xl))
                PrimaryButton(
                    text = if (state.isEditing) "Update dates" else "Tell the PG",
                    onClick = { viewModel.submit(note = null) },
                    enabled = state.canSubmit && !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.isEditing) {
                    Spacer(Modifier.height(HamsaTheme.spacing.sm))
                    SecondaryButton(
                        text = "Discard changes",
                        onClick = { viewModel.cancelEdit() },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item("reported") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                SectionHead("Reported")
                if (state.periods.isEmpty()) {
                    EmptyState(
                        title = "Nothing reported",
                        detail = "Tell the PG when you'll be away and it'll show up here.",
                        icon = Icons.Outlined.CalendarToday,
                    )
                } else {
                    state.periods.forEachIndexed { index, period ->
                        val nights = java.time.temporal.ChronoUnit.DAYS
                            .between(period.from, period.to)
                            .coerceAtLeast(0)
                        ListRow(
                            title = "${period.from.format(dayFormat)} → ${period.to.format(dayFormat)}",
                            subtitle = if (nights == 0L) "Same day" else "$nights night${if (nights == 1L) "" else "s"}",
                            showTopDivider = index > 0,
                            leading = { IconTile(icon = Icons.Outlined.NightsStay) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Edit leads: a wrong date is far more
                                    // common than a trip that never happened.
                                    IconButton(
                                        onClick = { viewModel.startEdit(period) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = "Change these dates",
                                            tint = colors.brand,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.remove(period) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Remove these away days",
                                            tint = colors.textTertiary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    picking?.let { target ->
        val initial = when (target) {
            Picking.From -> state.from
            Picking.To -> state.to ?: state.from
        } ?: LocalDate.now()

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                ModalBackdrop()
                PrimaryButton(
                    text = "Select",
                    size = ButtonSize.Small,
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            when (target) {
                                Picking.From -> viewModel.setFrom(date)
                                Picking.To -> viewModel.setTo(date)
                            }
                        }
                        picking = null
                    },
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = "Cancel",
                    size = ButtonSize.Small,
                    onClick = { picking = null },
                )
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
