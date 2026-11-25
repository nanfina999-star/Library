package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.LessonType
import com.example.data.local.SubjectEntity
import com.example.data.local.labelList
import com.example.ui.components.formatByteSize
import com.example.ui.components.formatFullDateTime
import com.example.ui.components.lessonTypeColor
import com.example.ui.components.lessonTypeIcon
import com.example.ui.components.lessonTypeLabel
import com.example.ui.components.subjectIconForKey

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentDetailsPane(
    lesson: LessonEntity,
    subject: SubjectEntity?,
    hasExternalShareableFile: Boolean,
    readTextFile: suspend (String?) -> String,
    onCategorize: () -> Unit,
    onEditNotes: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onAskInChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var noteFontSizeSp by remember { mutableFloatStateOf(16f) }
    var resolvedNoteText by remember(lesson.id, lesson.noteContent, lesson.internalFilePath) {
        mutableStateOf(lesson.noteContent)
    }

    LaunchedEffect(lesson.id, lesson.noteContent, lesson.internalFilePath) {
        resolvedNoteText = if (lesson.noteContent.isBlank() && lesson.type == LessonType.NOTE && lesson.internalFilePath != null) {
            readTextFile(lesson.internalFilePath)
        } else {
            lesson.noteContent
        }
    }

    val labels = remember(lesson.labels) { lesson.labelList() }
    val typeColor = lessonTypeColor(lesson.type)
    val subjectColor = subject?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Overview Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("document_overview_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(typeColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = lessonTypeIcon(lesson.type),
                                contentDescription = lessonTypeLabel(lesson.type),
                                tint = typeColor,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.stored_inside_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    if (lesson.description.isNotBlank()) {
                        Text(
                            text = lesson.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Category & Labels row
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (subject != null) {
                            Surface(
                                onClick = onCategorize,
                                shape = RoundedCornerShape(10.dp),
                                color = subjectColor.copy(alpha = 0.14f),
                                modifier = Modifier.testTag("details_category_chip"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(subjectColor),
                                    )
                                    Icon(
                                        imageVector = subjectIconForKey(subject.iconKey),
                                        contentDescription = null,
                                        tint = subjectColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = subjectColor,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        labels.forEach { label ->
                            Surface(
                                onClick = onCategorize,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Label,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }

                        if (labels.isEmpty()) {
                            Surface(
                                onClick = onCategorize,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Label,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "បន្ថែមស្លាក",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Document Metadata Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("document_metadata_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.details_metadata_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )

                    MetadataDetailRow(
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.details_created_at),
                        value = formatFullDateTime(lesson.createdAt),
                        valueTestTag = "metadata_created_at",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    MetadataDetailRow(
                        icon = Icons.Outlined.Update,
                        label = stringResource(R.string.details_updated_at),
                        value = formatFullDateTime(lesson.updatedAt),
                        valueTestTag = "metadata_updated_at",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    MetadataDetailRow(
                        icon = Icons.Outlined.Storage,
                        label = stringResource(R.string.details_file_size),
                        value = formatByteSize(lesson.fileSizeBytes),
                        valueTestTag = "metadata_file_size",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    val typeSummary = buildString {
                        append(lessonTypeLabel(lesson.type))
                        if (!lesson.mimeType.isNullOrBlank()) {
                            append(" • ${lesson.mimeType}")
                        }
                        if (lesson.type == LessonType.PDF && lesson.pageCount > 0) {
                            append(" • ${lesson.pageCount} ទំព័រ")
                            if (lesson.lastPageRead > 0) {
                                append(" (ទំព័រ ${lesson.lastPageRead + 1})")
                            }
                        }
                    }
                    MetadataDetailRow(
                        icon = lessonTypeIcon(lesson.type),
                        label = stringResource(R.string.details_file_type),
                        value = typeSummary,
                        valueTestTag = "metadata_file_type",
                    )

                    if (!lesson.originalFileName.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        MetadataDetailRow(
                            icon = Icons.Outlined.Description,
                            label = stringResource(R.string.details_original_name),
                            value = lesson.originalFileName,
                            valueTestTag = "metadata_original_name",
                        )
                    }
                }
            }

            // 3. Larger Notes Preview Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("large_notes_preview_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.details_notes_preview_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val charCount = resolvedNoteText.trim().length
                                val lineCount = if (resolvedNoteText.isBlank()) 0 else resolvedNoteText.lines().size
                                Text(
                                    text = if (charCount > 0) "$charCount តួអក្សរ • $lineCount បន្ទាត់" else "ទទេ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (resolvedNoteText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText(lesson.title, resolvedNoteText))
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.details_copied_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("details_copy_notes_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(R.string.details_copy_notes),
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onEditNotes,
                                modifier = Modifier.testTag("details_edit_notes_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.edit))
                            }
                        }
                    }

                    // Font scaling toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "ទំហំអក្សរ ${noteFontSizeSp.toInt()}sp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        OutlinedButton(
                            onClick = { noteFontSizeSp = (noteFontSizeSp - 2f).coerceAtLeast(13f) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("details_font_decrease_button"),
                        ) {
                            Text("A-")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = { noteFontSizeSp = (noteFontSizeSp + 2f).coerceAtMost(26f) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("details_font_increase_button"),
                        ) {
                            Text("A+")
                        }
                    }

                    // Generous scrollable notes preview surface
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        SelectionContainer {
                            Text(
                                text = resolvedNoteText.ifBlank {
                                    stringResource(R.string.details_notes_empty)
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = noteFontSizeSp.sp,
                                    lineHeight = (noteFontSizeSp * 1.6f).sp,
                                ),
                                color = if (resolvedNoteText.isNotBlank()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .padding(18.dp)
                                    .testTag("large_notes_preview_text"),
                            )
                        }
                    }
                }
            }

            // 4. Quick External / Share File Actions (when file-backed)
            if (hasExternalShareableFile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onOpenExternal,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("details_open_external_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.open_external), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("details_share_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.share), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (onAskInChat != null) {
                FilledTonalButton(
                    onClick = onAskInChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("details_ask_in_chat_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.chat_ask_in_chat))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MetadataDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueTestTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag(valueTestTag),
            )
        }
    }
}
