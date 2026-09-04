package com.aalaya.pg.ui.components

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aalaya.pg.ui.theme.FieldLabelTextStyle
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `.field label` — sentence-case label sitting *above* the input, not a
 * floating Material label. The design never uses a floating label, so
 * BasicTextField is used instead of OutlinedTextField to avoid Material's
 * built-in label animation and indicator line.
 */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = HamsaTheme.colors.textSecondary,
        modifier = modifier,
    )
}

/** `.fgroup > .flabel` — the tiny uppercase label above explorer dropdowns. */
@Composable
fun OverlineFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = FieldLabelTextStyle,
        color = HamsaTheme.colors.textSecondary,
        modifier = modifier,
    )
}

/**
 * `.field input` — 14dp radius, hairline border, brand border plus a 3dp
 * brand-subtle focus ring on focus (`:focus { box-shadow: 0 0 0 3px }`).
 */
@Composable
fun HamsaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helpText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = HamsaTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(HamsaTheme.radii.md)
    val isError = errorText != null
    val borderColor = when {
        isError -> colors.errorFg
        focused -> colors.brand
        else -> colors.border
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) FieldLabel(label)

        val selectionColors = TextSelectionColors(
            handleColor = colors.brand,
            backgroundColor = colors.brandMuted,
        )
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                interactionSource = interaction,
                textStyle = HamsaTheme.type.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.brand),
                visualTransformation =
                    if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    // Focus ring first so it sits outside the border.
                    .then(
                        if (focused && !isError) {
                            Modifier.border(3.dp, colors.brandSubtle, shape)
                        } else {
                            Modifier
                        },
                    )
                    .background(colors.surface, shape)
                    .border(1.dp, borderColor, shape)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            style = HamsaTheme.type.bodyLarge,
                            color = colors.textTertiary,
                        )
                    }
                    inner()
                },
            )
        }

        when {
            errorText != null -> Text(
                errorText,
                style = HamsaTheme.type.labelMedium,
                color = colors.errorFg,
            )
            helpText != null -> Text(
                helpText,
                style = HamsaTheme.type.labelMedium,
                color = colors.textTertiary,
            )
        }
    }
}

/**
 * A 6-digit one-time-code entry field, styled like [HamsaTextField] but
 * centered with wide letter-spacing so each digit reads distinctly.
 */
@Composable
fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    val colors = HamsaTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(HamsaTheme.radii.md)
    val borderColor = if (focused) colors.brand else colors.border

    BasicTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter(Char::isDigit).take(length)) },
        singleLine = true,
        interactionSource = interaction,
        textStyle = HamsaTheme.type.displaySmall.copy(
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = 8.sp,
        ),
        cursorBrush = SolidColor(colors.brand),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focused) Modifier.border(3.dp, colors.brandSubtle, shape) else Modifier)
            .background(colors.surface, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { inner() }
        },
    )
}

/** The search input at the top of Occupancy and Payments. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(HamsaTheme.radii.md)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, if (focused) colors.brand else colors.border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = HamsaTheme.type.bodyLarge, color = colors.textTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                interactionSource = interaction,
                textStyle = HamsaTheme.type.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.brand),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Without this, a query that matches nothing leaves the user
        // backspacing character by character to get their list back.
        if (value.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Clear search",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onValueChange("") },
            )
        }
    }
}

/**
 * `<input type="date">` — a read-only field showing the chosen date with a
 * trailing calendar icon. Tapping it is the caller's cue to open a date picker.
 */
@Composable
fun DateField(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(HamsaTheme.radii.md)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(colors.surface, shape)
                .border(1.dp, colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            Text(
                text = value ?: placeholder,
                style = HamsaTheme.type.bodyLarge,
                color = if (value != null) colors.textPrimary else colors.textTertiary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * `.fgroup` — uppercase label over a select. Backed by a DropdownMenu so the
 * cascading Branch → Floor → Room controls behave like the HTML `<select>`s.
 */
@Composable
fun <T> HamsaDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select…",
    enabled: Boolean = true,
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(HamsaTheme.radii.md)
    var open by remember { mutableStateOf(false) }
    // DropdownMenu sizes to its content, which leaves a narrow popup hanging off
    // a full-width field. Measure the anchor and match it, as the design's
    // `<select>` does.
    var anchorWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OverlineFieldLabel(label)
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .onGloballyPositioned {
                        anchorWidth = with(density) { it.size.width.toDp() }
                    }
                    .background(colors.surface, shape)
                    .border(1.dp, colors.border, shape)
                    .clickable(enabled = enabled) { open = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = selected?.let(optionLabel) ?: placeholder,
                    style = HamsaTheme.type.bodyLarge,
                    color = if (selected != null) colors.textPrimary else colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier
                    .background(colors.surface)
                    .then(if (anchorWidth > 0.dp) Modifier.width(anchorWidth) else Modifier),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                optionLabel(option),
                                style = HamsaTheme.type.bodyLarge,
                                color = colors.textPrimary,
                            )
                        },
                        onClick = { onSelect(option); open = false },
                    )
                }
            }
        }
    }
}

/**
 * `.seg` — the two/three-way segmented control ("Rooms & beds" / "Free beds
 * only"). The selected segment lifts onto a surface with brand text.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HamsaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.canvas, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val on = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (on) {
                            Modifier.hamsaSurface(
                                radius = 9.dp,
                                elevation = 2.dp,
                                color = colors.surface,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
            ) {
                Text(
                    text = option,
                    style = HamsaTheme.type.bodySmall.copy(
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (on) colors.brand else colors.textSecondary,
                )
            }
        }
    }
}
