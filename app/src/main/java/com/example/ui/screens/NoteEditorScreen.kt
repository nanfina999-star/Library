package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.labelList
import com.example.ui.components.CategoryAndLabelPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    existingLesson: LessonEntity?,
    subjects: List<SubjectEntity>,
    availableLabels: List<String>,
    initialSubjectId: Long?,
    onRequestAddCategory: () -> Unit,
    onBack: () -> Unit,
    onSave: (title: String, description: String, subjectId: Long, content: String, labels: List<String>) -> Unit,
) {
    var title by remember(existingLesson?.id) { mutableStateOf(existingLesson?.title.orEmpty()) }
    var description by remember(existingLesson?.id) { mutableStateOf(existingLesson?.description.orEmpty()) }
    var content by remember(existingLesson?.id) { mutableStateOf(existingLesson?.noteContent.orEmpty()) }
    var selectedLabels by remember(existingLesson?.id) {
        mutableStateOf(existingLesson?.labelList().orEmpty())
    }
    var selectedSubjectId by remember(existingLesson?.id, subjects, initialSubjectId) {
        mutableLongStateOf(
            existingLesson?.subjectId ?: initialSubjectId ?: subjects.firstOrNull()?.id ?: 1L
        )
    }

    LaunchedEffect(existingLesson) {
        if (existingLesson != null) {
            title = existingLesson.title
            description = existingLesson.description
            content = existingLesson.noteContent
            selectedLabels = existingLesson.labelList()
            selectedSubjectId = existingLesson.subjectId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingLesson == null) {
                            stringResource(R.string.note_editor_new_title)
                        } else {
                            stringResource(R.string.note_editor_edit_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("note_editor_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val finalTitle = title.trim().ifBlank { "កំណត់ត្រាមេរៀន" }
                            onSave(finalTitle, description, selectedSubjectId, content, selectedLabels)
                        },
                        enabled = title.isNotBlank() || content.isNotBlank(),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .testTag("save_note_button"),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text(stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.lesson_title_label)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.lesson_desc_label)) },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
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

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.note_content_label)) },
                placeholder = { Text(stringResource(R.string.note_content_hint)) },
                minLines = 10,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input"),
            )

            Text(
                text = "${content.length} តួអក្សរ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
