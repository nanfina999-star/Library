package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.LessonType
import com.example.data.local.SubjectEntity
import com.example.ui.LessonSortOrder
import com.example.ui.VaultStats
import com.example.ui.components.LessonCard
import com.example.ui.components.SortMenuChip
import com.example.ui.components.formatByteSize
import com.example.ui.components.lessonTypeIcon
import com.example.ui.components.lessonTypeLabel
import com.example.ui.components.subjectIconForKey
import com.example.ui.theme.LessonVaultTheme

@Composable
fun LibraryScreen(
    lessons: List<LessonEntity>,
    allLessonsCount: Int,
    subjects: List<SubjectEntity>,
    categoryCounts: Map<Long, Int>,
    availableLabels: List<String>,
    labelCounts: Map<String, Int> = emptyMap(),
    stats: VaultStats,
    searchQuery: String,
    selectedType: LessonType?,
    selectedSubjectId: Long?,
    selectedLabel: String?,
    sortOrder: LessonSortOrder,
    onSearchChange: (String) -> Unit,
    onSelectType: (LessonType?) -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onSelectLabel: (String?) -> Unit,
    onAddCategory: () -> Unit,
    onSelectSortOrder: (LessonSortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onOpenLesson: (LessonEntity) -> Unit,
    onTogglePin: (LessonEntity) -> Unit,
    onCategorizeLesson: (LessonEntity) -> Unit,
    onEditLesson: (LessonEntity) -> Unit,
    onDeleteLesson: (LessonEntity) -> Unit,
    onQuickImportDocument: () -> Unit,
    onQuickImportImage: () -> Unit,
    onQuickCreateNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subjectMap = subjects.associateBy { it.id }
    val hasActiveFilter = searchQuery.isNotBlank() ||
        selectedType != null ||
        selectedSubjectId != null ||
        selectedLabel != null

    Column(modifier = modifier.fillMaxSize()) {
        // Persistent Search Bar at the top of the main screen
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.testTag("library_search_clear_button"),
                        ) {
                            Icon(Icons.Outlined.Clear, contentDescription = "សម្អាត")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("library_search_input"),
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWide = maxWidth >= 680.dp

            if (isWide) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 310.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("saved_lessons_list"),
                ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryHeaderSection(
                        stats = stats,
                        allLessonsCount = allLessonsCount,
                        selectedType = selectedType,
                        selectedSubjectId = selectedSubjectId,
                        selectedLabel = selectedLabel,
                        subjects = subjects,
                        categoryCounts = categoryCounts,
                        availableLabels = availableLabels,
                        labelCounts = labelCounts,
                        onSelectType = onSelectType,
                        onSelectSubject = onSelectSubject,
                        onSelectLabel = onSelectLabel,
                        onAddCategory = onAddCategory,
                    )
                }

                if (lessons.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LibraryEmptyState(
                            isCompletelyEmpty = allLessonsCount == 0,
                            onImportDocument = onQuickImportDocument,
                            onImportImage = onQuickImportImage,
                            onCreateNote = onQuickCreateNote,
                            onClearFilters = onClearFilters,
                        )
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SavedDocumentsSectionHeader(
                            count = lessons.size,
                            sortOrder = sortOrder,
                            hasActiveFilter = hasActiveFilter,
                            onSelectSortOrder = onSelectSortOrder,
                            onClearFilters = onClearFilters,
                        )
                    }
                    items(lessons, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            subject = subjectMap[lesson.subjectId],
                            onClick = { onOpenLesson(lesson) },
                            onTogglePin = { onTogglePin(lesson) },
                            onEdit = { onEditLesson(lesson) },
                            onDelete = { onDeleteLesson(lesson) },
                            onCategorize = { onCategorizeLesson(lesson) },
                            onCategoryClick = { onSelectSubject(lesson.subjectId) },
                            onLabelClick = { label -> onSelectLabel(label) },
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("saved_lessons_list"),
            ) {
                item {
                    LibraryHeaderSection(
                        stats = stats,
                        allLessonsCount = allLessonsCount,
                        selectedType = selectedType,
                        selectedSubjectId = selectedSubjectId,
                        selectedLabel = selectedLabel,
                        subjects = subjects,
                        categoryCounts = categoryCounts,
                        availableLabels = availableLabels,
                        labelCounts = labelCounts,
                        onSelectType = onSelectType,
                        onSelectSubject = onSelectSubject,
                        onSelectLabel = onSelectLabel,
                        onAddCategory = onAddCategory,
                    )
                }

                if (lessons.isEmpty()) {
                    item {
                        LibraryEmptyState(
                            isCompletelyEmpty = allLessonsCount == 0,
                            onImportDocument = onQuickImportDocument,
                            onImportImage = onQuickImportImage,
                            onCreateNote = onQuickCreateNote,
                            onClearFilters = onClearFilters,
                        )
                    }
                } else {
                    item {
                        SavedDocumentsSectionHeader(
                            count = lessons.size,
                            sortOrder = sortOrder,
                            hasActiveFilter = hasActiveFilter,
                            onSelectSortOrder = onSelectSortOrder,
                            onClearFilters = onClearFilters,
                        )
                    }
                    items(lessons, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            subject = subjectMap[lesson.subjectId],
                            onClick = { onOpenLesson(lesson) },
                            onTogglePin = { onTogglePin(lesson) },
                            onEdit = { onEditLesson(lesson) },
                            onDelete = { onDeleteLesson(lesson) },
                            onCategorize = { onCategorizeLesson(lesson) },
                            onCategoryClick = { onSelectSubject(lesson.subjectId) },
                            onLabelClick = { label -> onSelectLabel(label) },
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SavedDocumentsSectionHeader(
    count: Int,
    sortOrder: LessonSortOrder,
    hasActiveFilter: Boolean,
    onSelectSortOrder: (LessonSortOrder) -> Unit,
    onClearFilters: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("saved_lessons_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "ឯកសារមេរៀនដែលបានរក្សាទុក",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (hasActiveFilter) {
                TextButton(onClick = onClearFilters) {
                    Text("សម្អាត")
                }
            }
            SortMenuChip(
                currentOrder = sortOrder,
                onSelectOrder = onSelectSortOrder,
            )
        }
    }
}

@Composable
private fun LibraryHeaderSection(
    stats: VaultStats,
    allLessonsCount: Int,
    selectedType: LessonType?,
    selectedSubjectId: Long?,
    selectedLabel: String?,
    subjects: List<SubjectEntity>,
    categoryCounts: Map<Long, Int>,
    availableLabels: List<String>,
    labelCounts: Map<String, Int>,
    onSelectType: (LessonType?) -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onSelectLabel: (String?) -> Unit,
    onAddCategory: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Editorial Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vault_banner),
                    contentDescription = stringResource(R.string.hero_title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xD907323D),
                                    Color(0xAA0C4E5E),
                                    Color(0xE607323D),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.stored_inside_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.hero_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.hero_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HeroStatPill(
                            value = "${stats.totalLessons}",
                            label = stringResource(R.string.stat_lessons),
                            modifier = Modifier.weight(1f),
                        )
                        HeroStatPill(
                            value = "${stats.totalSubjects}",
                            label = stringResource(R.string.stat_subjects),
                            modifier = Modifier.weight(1f),
                        )
                        HeroStatPill(
                            value = formatByteSize(stats.totalBytes),
                            label = stringResource(R.string.stat_storage),
                            modifier = Modifier.weight(1.25f),
                        )
                    }
                }
            }
        }

        // Category Filter Section (Math, History, Science, etc.)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "ប្រភេទមេរៀន (Categories)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("category_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedSubjectId == null,
                    onClick = { onSelectSubject(null) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text("ទាំងអស់ ($allLessonsCount)") },
                    modifier = Modifier.testTag("filter_category_all"),
                )

                subjects.forEach { subject ->
                    val dotColor = Color(subject.colorHex)
                    val count = categoryCounts[subject.id] ?: 0
                    val isSelected = selectedSubjectId == subject.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSubject(subject.id) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(dotColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = subjectIconForKey(subject.iconKey),
                                    contentDescription = null,
                                    tint = dotColor,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        },
                        label = {
                            Text(if (count > 0) "${subject.name} ($count)" else subject.name)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = dotColor.copy(alpha = 0.16f),
                            selectedLabelColor = dotColor,
                        ),
                        modifier = Modifier.testTag("filter_category_${subject.id}"),
                    )
                }

                AssistChip(
                    onClick = onAddCategory,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text("ប្រភេទថ្មី") },
                    modifier = Modifier.testTag("inline_add_category_chip"),
                )
            }
        }

        // File Type Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onSelectType(null) },
                label = { Text(stringResource(R.string.filter_all)) },
                modifier = Modifier.testTag("filter_type_all"),
            )
            LessonType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onSelectType(type) },
                    leadingIcon = {
                        Icon(
                            imageVector = lessonTypeIcon(type),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text(lessonTypeLabel(type)) },
                    modifier = Modifier.testTag("filter_type_${type.name.lowercase()}"),
                )
            }
        }

        // Labels Filter Row (appears when documents have labels)
        if (availableLabels.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ស្លាក (Labels)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("label_filter_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedLabel == null,
                        onClick = { onSelectLabel(null) },
                        label = { Text("គ្រប់ស្លាក") },
                        modifier = Modifier.testTag("filter_label_all"),
                    )
                    availableLabels.forEach { label ->
                        val tagCount = labelCounts.entries
                            .firstOrNull { it.key.equals(label, ignoreCase = true) }
                            ?.value ?: 0
                        FilterChip(
                            selected = selectedLabel.equals(label, ignoreCase = true),
                            onClick = { onSelectLabel(label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            label = { Text(if (tagCount > 0) "$label ($tagCount)" else label) },
                            modifier = Modifier.testTag("filter_label_$label"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryEmptyState(
    isCompletelyEmpty: Boolean,
    onImportDocument: () -> Unit,
    onImportImage: () -> Unit,
    onCreateNote: () -> Unit,
    onClearFilters: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_lessons),
                contentDescription = stringResource(R.string.empty_library_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(
                    if (isCompletelyEmpty) R.string.empty_library_title else R.string.empty_search_title,
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(
                    if (isCompletelyEmpty) R.string.empty_library_subtitle else R.string.empty_search_subtitle,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isCompletelyEmpty) {
                Button(
                    onClick = onImportDocument,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_import_document_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_import_document))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = onImportImage,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("empty_import_image_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("រូបភាព", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = onCreateNote,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("empty_create_note_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("កំណត់ត្រា", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("empty_clear_filters_button"),
                ) {
                    Icon(Icons.Outlined.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("សម្អាតតម្រងទាំងអស់")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    val sampleSubjects = listOf(
        SubjectEntity(id = 1L, name = "គណិតវិទ្យា", colorHex = 0xFFD97706, iconKey = "math"),
        SubjectEntity(id = 2L, name = "ប្រវត្តិសាស្ត្រ", colorHex = 0xFF86198F, iconKey = "history"),
    )
    val sampleLessons = listOf(
        LessonEntity(
            id = 1L,
            subjectId = 1L,
            title = "ជំពូកទី ១៖ អាំងតេក្រាលមិនកំណត់",
            description = "សៀវភៅលំហាត់ និងរូបមន្តសំខាន់ៗ",
            type = LessonType.PDF,
            fileSizeBytes = 2_450_000L,
            labels = "លំហាត់, ត្រៀមប្រឡង",
            pageCount = 14,
            lastPageRead = 3,
            isPinned = true,
        ),
        LessonEntity(
            id = 2L,
            subjectId = 2L,
            title = "សង្ខេបប្រវត្តិសាស្ត្រអង្គរ",
            description = "កំណត់ត្រាត្រៀមប្រឡងឆមាស",
            type = LessonType.NOTE,
            fileSizeBytes = 12_400L,
            noteContent = "សម័យអង្គរ...",
            labels = "សង្ខេប",
        ),
    )

    LessonVaultTheme {
        LibraryScreen(
            lessons = sampleLessons,
            allLessonsCount = sampleLessons.size,
            subjects = sampleSubjects,
            categoryCounts = mapOf(1L to 1, 2L to 1),
            availableLabels = listOf("លំហាត់", "សង្ខេប", "ត្រៀមប្រឡង"),
            stats = VaultStats(totalLessons = 2, totalSubjects = 2, totalBytes = 2_462_400L, pinnedCount = 1),
            searchQuery = "",
            selectedType = null,
            selectedSubjectId = null,
            selectedLabel = null,
            sortOrder = LessonSortOrder.DATE_CREATED,
            onSearchChange = {},
            onSelectType = {},
            onSelectSubject = {},
            onSelectLabel = {},
            onAddCategory = {},
            onSelectSortOrder = {},
            onClearFilters = {},
            onOpenLesson = {},
            onTogglePin = {},
            onCategorizeLesson = {},
            onEditLesson = {},
            onDeleteLesson = {},
            onQuickImportDocument = {},
            onQuickImportImage = {},
            onQuickCreateNote = {},
        )
    }
}
