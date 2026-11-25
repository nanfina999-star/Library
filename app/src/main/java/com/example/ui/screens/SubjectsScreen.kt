package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.SubjectEntity
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.components.LessonCard
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.components.formatByteSize
import com.example.ui.components.subjectIconForKey

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectsScreen(
    subjects: List<SubjectEntity>,
    allLessons: List<LessonEntity>,
    onOpenSubject: (SubjectEntity) -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (SubjectEntity) -> Unit,
    modifier: Modifier = Modifier,
    availableLabels: List<String> = emptyList(),
    labelCounts: Map<String, Int> = emptyMap(),
    onOpenLabel: (String) -> Unit = {},
    onRenameLabel: (String) -> Unit = {},
    onDeleteLabel: (String) -> Unit = {},
) {
    val lessonsBySubject = remember(allLessons) { allLessons.groupBy { it.subjectId } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_subjects),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "រៀបចំឯកសារមេរៀនតាមមុខវិជ្ជា និងស្លាក (Tags)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledTonalButton(
                onClick = onAddSubject,
                modifier = Modifier.testTag("add_subject_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("មុខវិជ្ជាថ្មី")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("subjects_hub_grid"),
        ) {
            items(subjects, key = { it.id }) { subject ->
                val list = lessonsBySubject[subject.id].orEmpty()
                val totalBytes = list.sumOf { it.fileSizeBytes }
                SubjectFolioCard(
                    subject = subject,
                    lessonCount = list.size,
                    totalBytes = totalBytes,
                    canDelete = subjects.size > 1,
                    onClick = { onOpenSubject(subject) },
                    onEdit = { onEditSubject(subject) },
                    onDelete = { onDeleteSubject(subject) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("tags_hub_section"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tags_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.tags_section_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                TagsHubContent(
                    availableLabels = availableLabels,
                    labelCounts = labelCounts,
                    onOpenLabel = onOpenLabel,
                    onRenameLabel = onRenameLabel,
                    onDeleteLabel = onDeleteLabel,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsHubContent(
    availableLabels: List<String>,
    labelCounts: Map<String, Int>,
    onOpenLabel: (String) -> Unit,
    onRenameLabel: (String) -> Unit,
    onDeleteLabel: (String) -> Unit,
) {
    if (availableLabels.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.tags_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableLabels.forEach { label ->
                val count = labelCounts.entries
                    .firstOrNull { it.key.equals(label, ignoreCase = true) }
                    ?.value ?: 0
                val isPersonal = label.equals("Personal", ignoreCase = true) || label == "ផ្ទាល់ខ្លួន"
                val containerColor = if (isPersonal) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                }
                val contentColor = if (isPersonal) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = containerColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = { onOpenLabel(label) },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
                            modifier = Modifier.testTag("hub_tag_chip_$label"),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (count > 0) "$label ($count)" else label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRenameLabel(label) },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("rename_tag_button_$label"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.rename_tag_title),
                                tint = contentColor,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        IconButton(
                            onClick = { onDeleteLabel(label) },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("delete_tag_button_$label"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.delete_tag_title),
                                tint = contentColor,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectFolioCard(
    subject: SubjectEntity,
    lessonCount: Int,
    totalBytes: Long,
    canDelete: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val spineColor = Color(subject.colorHex)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Book Spine Accent
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(142.dp)
                    .background(spineColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(spineColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = subjectIconForKey(subject.iconKey),
                            contentDescription = subject.name,
                            tint = spineColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "ជម្រើសមុខវិជ្ជា",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.detectGlobalUiTapSound(),
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                },
                            )
                            if (canDelete) {
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

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = spineColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "$lessonCount មេរៀន",
                            style = MaterialTheme.typography.labelSmall,
                            color = spineColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = formatByteSize(totalBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: SubjectEntity?,
    lessons: List<LessonEntity>,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onAddLessonToSubject: () -> Unit,
    onOpenLesson: (LessonEntity) -> Unit,
    onTogglePin: (LessonEntity) -> Unit,
    onCategorizeLesson: (LessonEntity) -> Unit,
    onEditLesson: (LessonEntity) -> Unit,
    onDeleteLesson: (LessonEntity) -> Unit,
) {
    val accent = subject?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (subject != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = subjectIconForKey(subject.iconKey),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = subject?.name ?: "មុខវិជ្ជា",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${lessons.size} ឯកសារមេរៀន",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subject_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    ThemeToggleIconButton(
                        isDarkTheme = isDarkTheme,
                        onToggle = onToggleTheme,
                    )
                    FilledTonalButton(
                        onClick = onAddLessonToSubject,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("subject_add_lesson_button"),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("បន្ថែម")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        if (lessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "មិនទាន់មានឯកសារក្នុងមុខវិជ្ជានេះទេ",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ចុចប៊ូតុង 'បន្ថែម' ខាងលើដើម្បីនាំចូល PDF រូបភាព ឬកំណត់ត្រាថ្មី",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(lessons, key = { it.id }) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        subject = subject,
                        onClick = { onOpenLesson(lesson) },
                        onTogglePin = { onTogglePin(lesson) },
                        onCategorize = { onCategorizeLesson(lesson) },
                        onEdit = { onEditLesson(lesson) },
                        onDelete = { onDeleteLesson(lesson) },
                    )
                }
            }
        }
    }
}
