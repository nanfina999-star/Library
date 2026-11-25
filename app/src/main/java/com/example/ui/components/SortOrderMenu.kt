package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.LessonSortOrder
import com.example.ui.audio.detectGlobalUiTapSound

@Composable
fun sortOrderLabel(order: LessonSortOrder): String = when (order) {
    LessonSortOrder.DATE_CREATED -> stringResource(R.string.sort_date_created)
    LessonSortOrder.NAME -> stringResource(R.string.sort_name)
    LessonSortOrder.CATEGORY -> stringResource(R.string.sort_category)
}

fun sortOrderIcon(order: LessonSortOrder): ImageVector = when (order) {
    LessonSortOrder.DATE_CREATED -> Icons.Outlined.CalendarToday
    LessonSortOrder.NAME -> Icons.Outlined.SortByAlpha
    LessonSortOrder.CATEGORY -> Icons.Outlined.Folder
}

fun sortOrderTestTag(order: LessonSortOrder): String = when (order) {
    LessonSortOrder.DATE_CREATED -> "sort_option_date_created"
    LessonSortOrder.NAME -> "sort_option_name"
    LessonSortOrder.CATEGORY -> "sort_option_category"
}

@Composable
fun TopBarSortMenuButton(
    currentOrder: LessonSortOrder,
    onSelectOrder: (LessonSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("main_sort_menu_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = stringResource(R.string.sort_menu_label),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        SortOrderDropdownMenu(
            expanded = expanded,
            currentOrder = currentOrder,
            onDismiss = { expanded = false },
            onSelectOrder = { order ->
                expanded = false
                onSelectOrder(order)
            },
        )
    }
}

@Composable
fun SortMenuChip(
    currentOrder: LessonSortOrder,
    onSelectOrder: (LessonSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = { Text(sortOrderLabel(currentOrder)) },
            modifier = Modifier.testTag("sort_lessons_chip"),
        )

        SortOrderDropdownMenu(
            expanded = expanded,
            currentOrder = currentOrder,
            onDismiss = { expanded = false },
            onSelectOrder = { order ->
                expanded = false
                onSelectOrder(order)
            },
        )
    }
}

@Composable
private fun SortOrderDropdownMenu(
    expanded: Boolean,
    currentOrder: LessonSortOrder,
    onDismiss: () -> Unit,
    onSelectOrder: (LessonSortOrder) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .detectGlobalUiTapSound()
            .testTag("sort_order_dropdown_menu"),
    ) {
        LessonSortOrder.entries.forEach { order ->
            val isSelected = order == currentOrder
            DropdownMenuItem(
                text = {
                    Text(
                        text = sortOrderLabel(order),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = sortOrderIcon(order),
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    null
                },
                onClick = { onSelectOrder(order) },
                modifier = Modifier.testTag(sortOrderTestTag(order)),
            )
        }
    }
}
