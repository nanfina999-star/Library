package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.SubjectEntity
import com.example.ui.components.LessonCard

@Composable
fun PinnedScreen(
    pinnedLessons: List<LessonEntity>,
    recentLessons: List<LessonEntity>,
    subjects: List<SubjectEntity>,
    onOpenLesson: (LessonEntity) -> Unit,
    onTogglePin: (LessonEntity) -> Unit,
    onCategorizeLesson: (LessonEntity) -> Unit,
    onEditLesson: (LessonEntity) -> Unit,
    onDeleteLesson: (LessonEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subjectMap = subjects.associateBy { it.id }
    val nonPinnedRecents = recentLessons.filter { !it.isPinned }.take(10)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.nav_pinned),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "មេរៀនសំខាន់ និងឯកសារដែលទើបអានថ្មីៗ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (pinnedLessons.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.empty_pinned_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_pinned_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "មេរៀនបានដាក់ម្ជុល (${pinnedLessons.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(pinnedLessons, key = { "pinned_${it.id}" }) { lesson ->
                LessonCard(
                    lesson = lesson,
                    subject = subjectMap[lesson.subjectId],
                    onClick = { onOpenLesson(lesson) },
                    onTogglePin = { onTogglePin(lesson) },
                    onCategorize = { onCategorizeLesson(lesson) },
                    onEdit = { onEditLesson(lesson) },
                    onDelete = { onDeleteLesson(lesson) },
                )
            }
        }

        if (nonPinnedRecents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "បើកអាន ឬកែប្រែថ្មីៗ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(nonPinnedRecents, key = { "recent_${it.id}" }) { lesson ->
                LessonCard(
                    lesson = lesson,
                    subject = subjectMap[lesson.subjectId],
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
