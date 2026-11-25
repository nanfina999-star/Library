package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.LessonType
import com.example.data.local.SubjectEntity
import com.example.data.local.labelList
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.theme.DocBadgeColor
import com.example.ui.theme.ImageBadgeColor
import com.example.ui.theme.NoteBadgeColor
import com.example.ui.theme.PdfBadgeColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SubjectIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

val SubjectIconOptions = listOf(
    SubjectIconOption("folder", "ទូទៅ", Icons.Outlined.Folder),
    SubjectIconOption("math", "គណិត", Icons.Outlined.Calculate),
    SubjectIconOption("history", "ប្រវត្តិ", Icons.Outlined.HistoryEdu),
    SubjectIconOption("science", "វិទ្យាសាស្ត្រ", Icons.Outlined.Science),
    SubjectIconOption("language", "ភាសា", Icons.Outlined.Translate),
    SubjectIconOption("code", "កូដ", Icons.Outlined.Code),
    SubjectIconOption("personal", "ផ្ទាល់ខ្លួន", Icons.Outlined.Person),
    SubjectIconOption("book", "សៀវភៅ", Icons.AutoMirrored.Outlined.MenuBook),
)

val PresetDocumentLabels = listOf(
    "Math",
    "Science",
    "Personal",
    "ផ្ទាល់ខ្លួន",
    "សង្ខេប",
    "លំហាត់",
    "ត្រៀមប្រឡង",
    "មេរៀនសំខាន់",
    "ឯកសារយោង",
)

fun subjectIconForKey(key: String): ImageVector =
    SubjectIconOptions.firstOrNull { it.key == key }?.icon ?: Icons.AutoMirrored.Outlined.MenuBook

fun lessonTypeIcon(type: LessonType): ImageVector = when (type) {
    LessonType.PDF -> Icons.Outlined.PictureAsPdf
    LessonType.IMAGE -> Icons.Outlined.Image
    LessonType.NOTE -> Icons.Outlined.Description
    LessonType.DOCUMENT -> Icons.AutoMirrored.Outlined.InsertDriveFile
}

fun lessonTypeColor(type: LessonType): Color = when (type) {
    LessonType.PDF -> PdfBadgeColor
    LessonType.IMAGE -> ImageBadgeColor
    LessonType.NOTE -> NoteBadgeColor
    LessonType.DOCUMENT -> DocBadgeColor
}

fun lessonTypeLabel(type: LessonType): String = when (type) {
    LessonType.PDF -> "PDF"
    LessonType.IMAGE -> "រូបភាព"
    LessonType.NOTE -> "កំណត់ត្រា"
    LessonType.DOCUMENT -> "ឯកសារ"
}

fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        return "${kb.toInt().coerceAtLeast(1)} KB"
    }
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}

fun formatShortDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun formatFullDateTime(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonCard(
    lesson: LessonEntity,
    subject: SubjectEntity?,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCategorize: (() -> Unit)? = null,
    onCategoryClick: (() -> Unit)? = null,
    onLabelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val badgeColor = lessonTypeColor(lesson.type)
    val subjectColor = subject?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val labels = remember(lesson.labels) { lesson.labelList() }
    val pinIconColor by animateColorAsState(
        targetValue = if (lesson.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pinColor",
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("lesson_card_${lesson.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (lesson.isPinned) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Left File Type Folio Badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = lessonTypeIcon(lesson.type),
                    contentDescription = lessonTypeLabel(lesson.type),
                    tint = badgeColor,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = lessonTypeLabel(lesson.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }

                    if (subject != null) {
                        Surface(
                            color = subjectColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = if (onCategoryClick != null) {
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onCategoryClick() }
                                    .testTag("card_category_badge_${lesson.id}")
                            } else {
                                Modifier
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(subjectColor),
                                )
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subjectColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (lesson.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(R.string.nav_pinned),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val previewSnippet = lesson.description.ifBlank {
                    if (lesson.type == LessonType.NOTE) lesson.noteContent.lineSequence().firstOrNull().orEmpty()
                    else lesson.originalFileName.orEmpty()
                }
                if (previewSnippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = previewSnippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (labels.isNotEmpty() || onCategorize != null) {
                    Spacer(modifier = Modifier.height(7.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        labels.forEach { label ->
                            val isPersonal = label.equals("Personal", ignoreCase = true) || label == "ផ្ទាល់ខ្លួន"
                            val chipContainer = if (isPersonal) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            }
                            val chipContent = if (isPersonal) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                            Surface(
                                color = chipContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (onLabelClick != null) {
                                            Modifier.clickable { onLabelClick(label) }
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .testTag("card_label_chip_${lesson.id}_$label"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Label,
                                        contentDescription = null,
                                        tint = chipContent,
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = chipContent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        if (onCategorize != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCategorize() }
                                    .testTag("card_quick_tag_button_${lesson.id}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = stringResource(R.string.quick_add_tag),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = if (labels.isEmpty()) stringResource(R.string.quick_add_tag) else "ស្លាក",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = formatByteSize(lesson.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (lesson.type == LessonType.PDF && lesson.pageCount > 0) {
                        Text(
                            text = if (lesson.lastPageRead > 0) {
                                "ទំព័រ ${lesson.lastPageRead + 1}/${lesson.pageCount}"
                            } else {
                                "${lesson.pageCount} ទំព័រ"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        text = formatShortDate(lesson.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pin_lesson_${lesson.id}"),
                ) {
                    Icon(
                        imageVector = if (lesson.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(R.string.nav_pinned),
                        tint = pinIconColor,
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("menu_lesson_${lesson.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "ជម្រើសបន្ថែម",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.detectGlobalUiTapSound(),
                    ) {
                        if (onCategorize != null) {
                            DropdownMenuItem(
                                text = { Text("ប្រភេទ និងស្លាក") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onCategorize()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}
