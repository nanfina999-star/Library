package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.labelList
import com.example.data.storage.ImportedVaultFile
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.theme.SubjectColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonActionSheet(
    onDismiss: () -> Unit,
    onImportDocument: () -> Unit,
    onImportImage: () -> Unit,
    onCreateNote: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .detectGlobalUiTapSound()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.action_add_lesson),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ActionSheetOptionCard(
                icon = Icons.Outlined.UploadFile,
                accentColor = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.action_import_document),
                subtitle = stringResource(R.string.action_import_document_desc),
                testTag = "sheet_import_document_button",
                onClick = {
                    onDismiss()
                    onImportDocument()
                },
            )

            ActionSheetOptionCard(
                icon = Icons.Outlined.PhotoLibrary,
                accentColor = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.action_import_image),
                subtitle = stringResource(R.string.action_import_image_desc),
                testTag = "sheet_import_image_button",
                onClick = {
                    onDismiss()
                    onImportImage()
                },
            )

            ActionSheetOptionCard(
                icon = Icons.Outlined.EditNote,
                accentColor = MaterialTheme.colorScheme.tertiary,
                title = stringResource(R.string.action_create_note),
                subtitle = stringResource(R.string.action_create_note_desc),
                testTag = "sheet_create_note_button",
                onClick = {
                    onDismiss()
                    onCreateNote()
                },
            )
        }
    }
}

@Composable
private fun ActionSheetOptionCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryAndLabelPicker(
    subjects: List<SubjectEntity>,
    selectedSubjectId: Long,
    onSelectSubject: (Long) -> Unit,
    selectedLabels: List<String>,
    availableLabels: List<String>,
    onLabelsChange: (List<String>) -> Unit,
    onRequestAddCategory: (() -> Unit)? = null,
) {
    var customLabelInput by remember { mutableStateOf("") }
    val allLabelOptions = remember(PresetDocumentLabels, availableLabels, selectedLabels) {
        (PresetDocumentLabels + availableLabels + selectedLabels)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    val submitCustomTags = {
        val parsed = customLabelInput.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (parsed.isNotEmpty()) {
            val merged = (selectedLabels + parsed).distinctBy { it.lowercase() }
            onLabelsChange(merged)
        }
        customLabelInput = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "ប្រភេទ / មុខវិជ្ជា (Category)",
            style = MaterialTheme.typography.labelLarge,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            subjects.forEach { subject ->
                val color = Color(subject.colorHex)
                FilterChip(
                    selected = subject.id == selectedSubjectId,
                    onClick = { onSelectSubject(subject.id) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    },
                    label = { Text(subject.name) },
                    modifier = Modifier.testTag("category_picker_chip_${subject.id}"),
                )
            }
            if (onRequestAddCategory != null) {
                AssistChip(
                    onClick = onRequestAddCategory,
                    leadingIcon = {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("ប្រភេទថ្មី") },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectedLabels.isNotEmpty()) {
                    "ស្លាកសម្គាល់ (${selectedLabels.size} បានជ្រើស)"
                } else {
                    "ស្លាកសម្គាល់ (Tags: Math, Science, Personal…)"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            if (selectedLabels.isNotEmpty()) {
                TextButton(
                    onClick = { onLabelsChange(emptyList()) },
                    modifier = Modifier.testTag("clear_selected_labels_button"),
                ) {
                    Text("សម្អាត")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = customLabelInput,
                onValueChange = { customLabelInput = it },
                placeholder = { Text("បន្ថែមស្លាកថ្មី (ឧ. Math, ឆមាស១)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitCustomTags() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_label_input"),
            )
            FilledTonalIconButton(
                onClick = submitCustomTags,
                enabled = customLabelInput.isNotBlank(),
                modifier = Modifier.testTag("add_custom_label_button"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "បន្ថែមស្លាក")
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            allLabelOptions.forEach { label ->
                val isSelected = selectedLabels.any { it.equals(label, ignoreCase = true) }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = if (isSelected) {
                            selectedLabels.filterNot { it.equals(label, ignoreCase = true) }
                        } else {
                            selectedLabels + label
                        }
                        onLabelsChange(updated)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSelected) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.Label,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text(label) },
                    modifier = Modifier.testTag("label_picker_chip_$label"),
                )
            }
        }
    }
}

@Composable
fun ImportLessonDialog(
    staged: ImportedVaultFile,
    subjects: List<SubjectEntity>,
    availableLabels: List<String>,
    initialSubjectId: Long?,
    onRequestAddCategory: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, subjectId: Long, studyNote: String, labels: List<String>) -> Unit,
) {
    val defaultTitle = remember(staged.originalFileName) {
        staged.originalFileName.substringBeforeLast('.').ifBlank { staged.originalFileName }
    }
    var title by remember { mutableStateOf(defaultTitle) }
    var description by remember { mutableStateOf("") }
    var studyNote by remember { mutableStateOf("") }
    var selectedLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSubjectId by remember(subjects, initialSubjectId) {
        mutableLongStateOf(initialSubjectId ?: subjects.firstOrNull()?.id ?: 1L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.detectGlobalUiTapSound(),
        title = {
            Text(
                text = stringResource(R.string.import_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = lessonTypeIcon(staged.detectedType),
                            contentDescription = null,
                            tint = lessonTypeColor(staged.detectedType),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = staged.originalFileName,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                            val meta = buildString {
                                append(lessonTypeLabel(staged.detectedType))
                                append(" • ")
                                append(formatByteSize(staged.fileSizeBytes))
                                if (staged.pageCount > 0) {
                                    append(" • ${staged.pageCount} ទំព័រ")
                                }
                            }
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.lesson_title_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_title_input"),
                )

                CategoryAndLabelPicker(
                    subjects = subjects,
                    selectedSubjectId = selectedSubjectId,
                    onSelectSubject = { selectedSubjectId = it },
                    selectedLabels = selectedLabels,
                    availableLabels = availableLabels,
                    onLabelsChange = { selectedLabels = it },
                    onRequestAddCategory = onRequestAddCategory,
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.lesson_desc_label)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = studyNote,
                    onValueChange = { studyNote = it },
                    label = { Text(stringResource(R.string.lesson_note_label)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, selectedSubjectId, studyNote, selectedLabels) },
                modifier = Modifier.testTag("confirm_import_button"),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun EditLessonMetadataDialog(
    lesson: LessonEntity,
    subjects: List<SubjectEntity>,
    availableLabels: List<String>,
    onRequestAddCategory: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, subjectId: Long, labels: List<String>) -> Unit,
) {
    var title by remember(lesson.id) { mutableStateOf(lesson.title) }
    var description by remember(lesson.id) { mutableStateOf(lesson.description) }
    var selectedSubjectId by remember(lesson.id) { mutableLongStateOf(lesson.subjectId) }
    var selectedLabels by remember(lesson.id) { mutableStateOf(lesson.labelList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.detectGlobalUiTapSound(),
        title = { Text(stringResource(R.string.edit)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.lesson_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.lesson_desc_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                CategoryAndLabelPicker(
                    subjects = subjects,
                    selectedSubjectId = selectedSubjectId,
                    onSelectSubject = { selectedSubjectId = it },
                    selectedLabels = selectedLabels,
                    availableLabels = availableLabels,
                    onLabelsChange = { selectedLabels = it },
                    onRequestAddCategory = onRequestAddCategory,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, selectedSubjectId, selectedLabels) },
                enabled = title.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun QuickCategorizeDialog(
    lesson: LessonEntity,
    subjects: List<SubjectEntity>,
    availableLabels: List<String>,
    onRequestAddCategory: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, labels: List<String>) -> Unit,
) {
    var selectedSubjectId by remember(lesson.id) { mutableLongStateOf(lesson.subjectId) }
    var selectedLabels by remember(lesson.id) { mutableStateOf(lesson.labelList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.detectGlobalUiTapSound(),
        title = {
            Column {
                Text("ប្រភេទ និងស្លាកឯកសារ", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                CategoryAndLabelPicker(
                    subjects = subjects,
                    selectedSubjectId = selectedSubjectId,
                    onSelectSubject = { selectedSubjectId = it },
                    selectedLabels = selectedLabels,
                    availableLabels = availableLabels,
                    onLabelsChange = { selectedLabels = it },
                    onRequestAddCategory = onRequestAddCategory,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedSubjectId, selectedLabels) },
                modifier = Modifier.testTag("confirm_categorize_button"),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectEditorDialog(
    existingSubject: SubjectEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: Long, iconKey: String) -> Unit,
) {
    var name by remember(existingSubject?.id) { mutableStateOf(existingSubject?.name.orEmpty()) }
    var selectedColor by remember(existingSubject?.id) {
        mutableLongStateOf(existingSubject?.colorHex ?: SubjectColors.first())
    }
    var selectedIcon by remember(existingSubject?.id) {
        mutableStateOf(existingSubject?.iconKey ?: SubjectIconOptions.first().key)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.detectGlobalUiTapSound(),
        title = {
            Text(
                text = if (existingSubject == null) {
                    stringResource(R.string.add_subject)
                } else {
                    stringResource(R.string.edit_subject)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.subject_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input"),
                )

                Text(
                    text = stringResource(R.string.subject_color_label),
                    style = MaterialTheme.typography.labelLarge,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SubjectColors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.subject_icon_label),
                    style = MaterialTheme.typography.labelLarge,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SubjectIconOptions.forEach { option ->
                        FilterChip(
                            selected = selectedIcon == option.key,
                            onClick = { selectedIcon = option.key },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedColor, selectedIcon) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_subject_button"),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.detectGlobalUiTapSound(),
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                ),
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun RenameTagDialog(
    currentTag: String,
    onDismiss: () -> Unit,
    onConfirm: (newTag: String) -> Unit,
) {
    var tagInput by remember(currentTag) { mutableStateOf(currentTag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.detectGlobalUiTapSound(),
        title = { Text(stringResource(R.string.rename_tag_title)) },
        text = {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text(stringResource(R.string.rename_tag_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_tag_input"),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tagInput.trim().replace(",", "")) },
                enabled = tagInput.trim().replace(",", "").isNotBlank(),
                modifier = Modifier.testTag("confirm_rename_tag_button"),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
