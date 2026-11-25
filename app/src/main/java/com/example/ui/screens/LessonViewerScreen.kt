package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.LessonEntity
import com.example.data.local.LessonType
import com.example.data.local.SubjectEntity
import com.example.data.local.labelList
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.components.formatByteSize
import com.example.ui.components.formatFullDateTime
import com.example.ui.components.formatShortDate
import com.example.ui.components.lessonTypeColor
import com.example.ui.components.lessonTypeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import kotlin.math.roundToInt

private class PdfDocumentSession(file: File) : Closeable {
    private val pfd: ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer = PdfRenderer(pfd)
    private val mutex = Mutex()
    private val cache = object : LruCache<Int, Bitmap>(6) {}

    val pageCount: Int get() = renderer.pageCount

    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int = 1280): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null
        cache.get(pageIndex)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }
        mutex.withLock {
            cache.get(pageIndex)?.let { cached ->
                if (!cached.isRecycled) return@withLock cached
            }
            renderer.openPage(pageIndex).use { page ->
                val aspect = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
                val width = targetWidthPx.coerceIn(600, 1600)
                val height = (width * aspect).roundToInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(pageIndex, bitmap)
                bitmap
            }
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { pfd.close() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonViewerScreen(
    lesson: LessonEntity?,
    subject: SubjectEntity?,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    resolveFile: (String?) -> File?,
    getShareUri: (String?) -> Uri?,
    readTextFile: suspend (String?) -> String,
    onBack: () -> Unit,
    onTogglePin: (LessonEntity) -> Unit,
    onCategorize: (LessonEntity) -> Unit,
    onEditNoteLesson: (LessonEntity) -> Unit,
    onEditMetadata: (LessonEntity) -> Unit,
    onUpdateLastPage: (Long, Int) -> Unit,
    onSaveStudyNotes: (Long, String) -> Unit,
    onDeleteLesson: (LessonEntity) -> Unit,
    onAskInChat: ((LessonEntity) -> Unit)? = null,
) {
    val context = LocalContext.current
    var showNotesSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedTabIndex by remember(lesson?.id) { mutableIntStateOf(0) }

    if (lesson == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val storedFile = remember(lesson.internalFilePath) { resolveFile(lesson.internalFilePath) }
    val shareUri = remember(lesson.internalFilePath) { getShareUri(lesson.internalFilePath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = lessonTypeLabel(lesson.type),
                                style = MaterialTheme.typography.labelSmall,
                                color = lessonTypeColor(lesson.type),
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (subject != null) {
                                Text(
                                    text = "• ${subject.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("viewer_back_button")) {
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
                    if (onAskInChat != null) {
                        IconButton(
                            onClick = { onAskInChat(lesson) },
                            modifier = Modifier.testTag("viewer_ask_in_chat_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = stringResource(R.string.chat_ask_in_chat),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(
                        onClick = { onTogglePin(lesson) },
                        modifier = Modifier.testTag("viewer_pin_button"),
                    ) {
                        Icon(
                            imageVector = if (lesson.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(R.string.nav_pinned),
                            tint = if (lesson.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (lesson.type == LessonType.NOTE && lesson.internalFilePath == null) {
                        IconButton(
                            onClick = { onEditNoteLesson(lesson) },
                            modifier = Modifier.testTag("viewer_edit_note_button"),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    } else {
                        IconButton(
                            onClick = { onEditMetadata(lesson) },
                            modifier = Modifier.testTag("viewer_edit_meta_button"),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("viewer_delete_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { showNotesSheet = true },
                            modifier = Modifier.testTag("viewer_study_notes_button"),
                        ) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (lesson.noteContent.isNotBlank() && lesson.type != LessonType.NOTE) {
                                    "កំណត់ត្រា (មាន)"
                                } else {
                                    "កំណត់ត្រាសិក្សា"
                                },
                            )
                        }

                        OutlinedButton(
                            onClick = { onCategorize(lesson) },
                            modifier = Modifier.testTag("viewer_categorize_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Label,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ប្រភេទ / ស្លាក")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (shareUri != null) {
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = lesson.mimeType ?: "*/*"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        putExtra(Intent.EXTRA_SUBJECT, lesson.title)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, context.getString(R.string.share)),
                                    )
                                },
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share))
                            }

                            IconButton(
                                onClick = {
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(shareUri, lesson.mimeType ?: "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(viewIntent)
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = stringResource(R.string.open_external),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        val openExternalAction = {
            shareUri?.let { uri ->
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, lesson.mimeType ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(viewIntent) }
            }
            Unit
        }
        val shareAction = {
            shareUri?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = lesson.mimeType ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, lesson.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, context.getString(R.string.share)),
                )
            }
            Unit
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.viewer_tab_preview)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.testTag("viewer_tab_preview"),
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.viewer_tab_details)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.testTag("viewer_tab_details"),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (selectedTabIndex == 1) {
                    DocumentDetailsPane(
                        lesson = lesson,
                        subject = subject,
                        hasExternalShareableFile = shareUri != null,
                        readTextFile = readTextFile,
                        onCategorize = { onCategorize(lesson) },
                        onEditNotes = {
                            if (lesson.type == LessonType.NOTE && lesson.internalFilePath == null) {
                                onEditNoteLesson(lesson)
                            } else {
                                showNotesSheet = true
                            }
                        },
                        onOpenExternal = openExternalAction,
                        onShare = shareAction,
                        onAskInChat = onAskInChat?.let { cb -> { cb(lesson) } },
                    )
                } else {
                    when (lesson.type) {
                        LessonType.PDF -> {
                            if (storedFile != null) {
                                PdfViewerPane(
                                    file = storedFile,
                                    initialPage = lesson.lastPageRead,
                                    onPageChanged = { page -> onUpdateLastPage(lesson.id, page) },
                                )
                            } else {
                                MissingFilePane()
                            }
                        }

                        LessonType.IMAGE -> {
                            if (storedFile != null) {
                                ImageViewerPane(
                                    file = storedFile,
                                    title = lesson.title,
                                    description = lesson.description,
                                )
                            } else {
                                MissingFilePane()
                            }
                        }

                        LessonType.NOTE -> {
                            NoteViewerPane(
                                lesson = lesson,
                                subject = subject,
                                readTextFile = readTextFile,
                                onEditNote = {
                                    if (lesson.internalFilePath == null) {
                                        onEditNoteLesson(lesson)
                                    } else {
                                        showNotesSheet = true
                                    }
                                },
                            )
                        }

                        LessonType.DOCUMENT -> {
                            DocumentInfoPane(
                                lesson = lesson,
                                subject = subject,
                                onEditNotes = { showNotesSheet = true },
                                onOpenExternal = openExternalAction,
                                onShare = shareAction,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotesSheet) {
        StudyNotesBottomSheet(
            initialNotes = lesson.noteContent,
            onDismiss = { showNotesSheet = false },
            onSave = { updated ->
                onSaveStudyNotes(lesson.id, updated)
                showNotesSheet = false
            },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_lesson_title),
            message = stringResource(R.string.delete_lesson_message),
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDeleteLesson(lesson)
            },
        )
    }
}

@Composable
private fun PdfViewerPane(
    file: File,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
) {
    val session = remember(file.absolutePath) {
        runCatching { PdfDocumentSession(file) }.getOrNull()
    }
    DisposableEffect(session) {
        onDispose { session?.close() }
    }

    if (session == null || session.pageCount <= 0) {
        MissingFilePane(message = "មិនអាចបើកឯកសារ PDF នេះបានទេ")
        return
    }

    var currentPage by remember(file.absolutePath) {
        mutableIntStateOf(initialPage.coerceIn(0, session.pageCount - 1))
    }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(currentPage, session) {
        isLoading = true
        pageBitmap = session.renderPage(currentPage)
        isLoading = false
        onPageChanged(currentPage)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Page Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .pointerInput(currentPage) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = nextScale
                        offset = if (nextScale > 1f) {
                            Offset(
                                x = (offset.x + pan.x).coerceIn(-600f, 600f),
                                y = (offset.y + pan.y).coerceIn(-900f, 900f),
                            )
                        } else {
                            Offset.Zero
                        }
                    }
                }
                .pointerInput(currentPage) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2f
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val bmp = pageBitmap
            if (bmp != null && !bmp.isRecycled) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPage + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
            }
            if (isLoading) {
                CircularProgressIndicator()
            }

            if (scale > 1.05f) {
                Surface(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.ZoomOutMap, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("${(scale * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Page Navigation Controls
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            if (currentPage > 0) {
                                scale = 1f
                                offset = Offset.Zero
                                currentPage--
                            }
                        },
                        enabled = currentPage > 0,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ទំព័រមុន")
                    }

                    Text(
                        text = "ទំព័រ ${currentPage + 1} / ${session.pageCount}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    FilledTonalIconButton(
                        onClick = {
                            if (currentPage < session.pageCount - 1) {
                                scale = 1f
                                offset = Offset.Zero
                                currentPage++
                            }
                        },
                        enabled = currentPage < session.pageCount - 1,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ទំព័របន្ទាប់")
                    }
                }

                if (session.pageCount > 1) {
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { next ->
                            val target = next.roundToInt().coerceIn(0, session.pageCount - 1)
                            if (target != currentPage) {
                                scale = 1f
                                offset = Offset.Zero
                                currentPage = target
                            }
                        },
                        valueRange = 0f..(session.pageCount - 1).toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageViewerPane(
    file: File,
    title: String,
    description: String,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1316))
            .pointerInput(file.absolutePath) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = nextScale
                    offset = if (nextScale > 1f) {
                        Offset(
                            x = (offset.x + pan.x).coerceIn(-900f, 900f),
                            y = (offset.y + pan.y).coerceIn(-1200f, 1200f),
                        )
                    } else {
                        Offset.Zero
                    }
                }
            }
            .pointerInput(file.absolutePath) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.4f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )

        if (scale > 1.05f) {
            Surface(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.68f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.ZoomOutMap,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${(scale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }

        if (description.isNotBlank()) {
            Surface(
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteViewerPane(
    lesson: LessonEntity,
    subject: SubjectEntity?,
    readTextFile: suspend (String?) -> String,
    onEditNote: () -> Unit,
) {
    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    var fileBackedText by remember(lesson.id, lesson.internalFilePath) {
        mutableStateOf(lesson.noteContent)
    }

    LaunchedEffect(lesson.id, lesson.internalFilePath, lesson.noteContent) {
        if (lesson.noteContent.isBlank() && lesson.internalFilePath != null) {
            fileBackedText = readTextFile(lesson.internalFilePath)
        } else {
            fileBackedText = lesson.noteContent
        }
    }

    val labels = remember(lesson.labels) { lesson.labelList() }
    val subjectColor = subject?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Metadata strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${stringResource(R.string.details_created_at)}៖ ${formatFullDateTime(lesson.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = formatByteSize(lesson.fileSizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Update,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${stringResource(R.string.details_updated_at)}៖ ${formatShortDate(lesson.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (subject != null || labels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (subject != null) {
                            Surface(
                                color = subjectColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                                    )
                                }
                            }
                        }
                        labels.forEach { label ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${fileBackedText.trim().length} តួអក្សរ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(13f) },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("A-")
                }
                OutlinedButton(
                    onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(26f) },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("A+")
                }
                FilledTonalButton(
                    onClick = onEditNote,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.edit))
                }
            }
        }

        if (lesson.description.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            SelectionContainer {
                Text(
                    text = fileBackedText.ifBlank { "មិនទាន់មានខ្លឹមសារកំណត់ត្រានៅឡើយទេ។" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.6f).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentInfoPane(
    lesson: LessonEntity,
    subject: SubjectEntity?,
    onEditNotes: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
) {
    val subjectColor = subject?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val labels = remember(lesson.labels) { lesson.labelList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(lessonTypeColor(lesson.type).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = lessonTypeColor(lesson.type),
                modifier = Modifier.size(44.dp),
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = stringResource(R.string.stored_inside_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }

        Text(
            text = lesson.originalFileName ?: lesson.title,
            style = MaterialTheme.typography.titleLarge,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${formatByteSize(lesson.fileSizeBytes)} • ${lesson.mimeType ?: "ឯកសារ"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "• ${stringResource(R.string.details_created_at)}៖ ${formatFullDateTime(lesson.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (subject != null || labels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (subject != null) {
                    Surface(
                        color = subjectColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = subjectColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                labels.forEach { label ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        if (lesson.description.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onOpenExternal,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.open_external), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.share), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Larger study notes preview card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.study_notes_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onEditNotes) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.edit))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        text = lesson.noteContent.ifBlank { stringResource(R.string.details_notes_empty) },
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                        color = if (lesson.noteContent.isNotBlank()) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingFilePane(
    message: String = "រកមិនឃើញឯកសារនៅក្នុងឃ្លាំងផ្ទុកទេ",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyNotesBottomSheet(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var notes by remember(initialNotes) { mutableStateOf(initialNotes) }

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
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.study_notes_title),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text(stringResource(R.string.study_notes_hint)) },
                minLines = 6,
                maxLines = 12,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_notes_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSave(notes) },
                    modifier = Modifier.testTag("save_study_notes_button"),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
