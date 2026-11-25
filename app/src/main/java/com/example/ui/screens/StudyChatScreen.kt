package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.cloud.ChatMessage
import com.example.data.cloud.DirectLessonMessage
import com.example.data.cloud.FriendConnection
import com.example.data.cloud.LessonAttachmentDraft
import com.example.data.cloud.StudentProfile
import com.example.data.cloud.StudyRoom
import com.example.data.cloud.UserAccount
import com.example.data.cloud.VOICE_DEFAULT_LABEL_PREFIX
import com.example.data.cloud.VoiceAttachmentDraft
import com.example.data.local.LessonEntity
import com.example.data.local.SubjectEntity
import com.example.ui.UiState
import com.example.ui.audio.LiveVoiceRecordingBar
import com.example.ui.audio.VoiceMessagePlayerCard
import com.example.ui.audio.VoicePlaybackController
import com.example.ui.audio.VoiceRecorderController
import com.example.ui.audio.VoiceRecordIconButton
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.audio.rememberVoicePlaybackController
import com.example.ui.components.EditStudentAccountDialog
import com.example.ui.components.StudentAccountFormCard
import com.example.ui.components.StudentAccountUnlockCard
import com.example.ui.components.formatFullDateTime
import com.example.ui.components.lessonTypeIcon
import com.example.ui.components.lessonTypeLabel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyChatScreen(
    currentUserId: String,
    suggestedName: String,
    accountState: UiState<UserAccount?>,
    roomsState: UiState<List<StudyRoom>>,
    messagesState: UiState<List<ChatMessage>>,
    selectedRoomId: String?,
    isAccountLocked: Boolean,
    isSavingAccount: Boolean,
    isSendingMessage: Boolean,
    pendingAttachment: LessonAttachmentDraft?,
    localLessons: List<LessonEntity>,
    localSubjects: List<SubjectEntity>,
    onSaveAccount: (displayName: String, gradeLevel: String, password: String, existing: UserAccount?) -> Unit,
    onUnlockAccount: (UserAccount, String) -> Boolean,
    onLockAccount: () -> Unit,
    onSignOut: () -> Unit,
    onSelectRoom: (String) -> Unit,
    onCreateRoom: (name: String, description: String, subjectTag: String) -> Unit,
    onAttachLesson: (LessonEntity, SubjectEntity?) -> Unit,
    onClearAttachment: () -> Unit,
    onSendMessage: (UserAccount, String, () -> Unit) -> Unit,
    onDeleteMessage: (String) -> Unit,
    studentDirectoryState: UiState<List<StudentProfile>> = UiState.Success(emptyList()),
    friendConnectionsState: UiState<List<FriendConnection>> = UiState.Success(emptyList()),
    directMessagesState: UiState<List<DirectLessonMessage>> = UiState.Success(emptyList()),
    selectedFriendConnectionId: String? = null,
    onSelectFriendConnection: (String?) -> Unit = {},
    onSendFriendRequest: (UserAccount, StudentProfile, FriendConnection?) -> Unit = { _, _, _ -> },
    onRespondFriendRequest: (FriendConnection, UserAccount, Boolean) -> Unit = { _, _, _ -> },
    onRemoveFriendConnection: (String) -> Unit = {},
    onSendDirectMessage: (FriendConnection, UserAccount, String, () -> Unit) -> Unit = { _, _, _, _ -> },
    onDeleteDirectMessage: (String, String) -> Unit = { _, _ -> },
    onSendVoiceMessage: (UserAccount, VoiceAttachmentDraft, String, () -> Unit) -> Unit = { _, _, _, _ -> },
    onSendDirectVoiceMessage: (FriendConnection, UserAccount, VoiceAttachmentDraft, String, () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onShowInfoMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showLessonPickerSheet by remember { mutableStateOf(false) }
    var selectedChatMode by remember {
        mutableIntStateOf(if (selectedFriendConnectionId != null) 1 else 0)
    }

    LaunchedEffect(selectedFriendConnectionId) {
        if (selectedFriendConnectionId != null) {
            selectedChatMode = 1
        }
    }

    when (accountState) {
        UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = accountState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            return
        }

        is UiState.Success -> {
            val account = accountState.data
            if (account == null) {
                StudentAccountFormCard(
                    existingAccount = null,
                    suggestedName = suggestedName,
                    isSaving = isSavingAccount,
                    onSave = { name, grade, password ->
                        onSaveAccount(name, grade, password, null)
                    },
                    onSignOut = onSignOut,
                    modifier = modifier,
                )
                return
            }

            if (isAccountLocked) {
                StudentAccountUnlockCard(
                    account = account,
                    onUnlock = { pwd -> onUnlockAccount(account, pwd) },
                    onSignOut = onSignOut,
                    modifier = modifier,
                )
                return
            }

            val subjectMap = remember(localSubjects) { localSubjects.associateBy { it.id } }
            var draftMessage by remember { mutableStateOf("") }
            val listState = rememberLazyListState()
            val messages = (messagesState as? UiState.Success)?.data.orEmpty()

            val roomVoicePlayer = rememberVoicePlaybackController()
            val roomVoiceRecorder = remember(context) { VoiceRecorderController(context) }
            var roomRecordingSeconds by remember { mutableIntStateOf(0) }
            val permissionDeniedMsg = stringResource(R.string.voice_permission_denied)

            DisposableEffect(roomVoiceRecorder) {
                onDispose { roomVoiceRecorder.release() }
            }

            LaunchedEffect(roomVoiceRecorder.isRecording, account.userId, selectedRoomId) {
                if (roomVoiceRecorder.isRecording) {
                    roomRecordingSeconds = 0
                    while (roomVoiceRecorder.isRecording && roomRecordingSeconds < 45) {
                        delay(1000L)
                        roomRecordingSeconds++
                    }
                    if (roomVoiceRecorder.isRecording) {
                        val draft = roomVoiceRecorder.stopAndBuildDraft()
                        if (draft != null && selectedRoomId != null) {
                            onSendVoiceMessage(account, draft, draftMessage) { draftMessage = "" }
                        }
                    }
                } else {
                    roomRecordingSeconds = 0
                }
            }

            LaunchedEffect(messages.size, selectedRoomId) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }

            Column(modifier = modifier.fillMaxSize()) {
                // Student Account Header Strip
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = account.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = account.gradeLevel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { showEditAccountDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("edit_student_account_button"),
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.account_edit_title))
                            }
                            IconButton(
                                onClick = onLockAccount,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("lock_student_account_button"),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = stringResource(R.string.account_lock_button))
                            }
                        }
                    }
                }

                val incomingRequestsCount = remember(friendConnectionsState, currentUserId) {
                    (friendConnectionsState as? UiState.Success)?.data
                        ?.count { it.status == "pending" && it.recipientId == currentUserId }
                        ?: 0
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedChatMode,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Tab(
                        selected = selectedChatMode == 0,
                        onClick = { selectedChatMode = 0 },
                        modifier = Modifier.testTag("chat_mode_rooms_tab"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Forum,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(stringResource(R.string.chat_tab_rooms))
                            }
                        },
                    )
                    Tab(
                        selected = selectedChatMode == 1,
                        onClick = { selectedChatMode = 1 },
                        modifier = Modifier.testTag("chat_mode_friends_tab"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(stringResource(R.string.chat_tab_friends))
                                if (incomingRequestsCount > 0) {
                                    Badge {
                                        Text(incomingRequestsCount.toString())
                                    }
                                }
                            }
                        },
                    )
                }

                if (selectedChatMode == 1) {
                    FriendsChatPane(
                        currentUserId = currentUserId,
                        myAccount = account,
                        directoryState = studentDirectoryState,
                        connectionsState = friendConnectionsState,
                        directMessagesState = directMessagesState,
                        selectedConnectionId = selectedFriendConnectionId,
                        isSendingMessage = isSendingMessage,
                        pendingAttachment = pendingAttachment,
                        onSelectConnection = onSelectFriendConnection,
                        onSendFriendRequest = { targetStudent, existingConn ->
                            onSendFriendRequest(account, targetStudent, existingConn)
                        },
                        onAcceptRequest = { req ->
                            onRespondFriendRequest(req, account, true)
                        },
                        onDeclineRequest = { req ->
                            onRespondFriendRequest(req, account, false)
                        },
                        onRemoveConnection = onRemoveFriendConnection,
                        onOpenLessonPicker = { showLessonPickerSheet = true },
                        onClearAttachment = onClearAttachment,
                        onSendDirectMessage = { conn, text, onSent ->
                            onSendDirectMessage(conn, account, text, onSent)
                        },
                        onDeleteDirectMessage = onDeleteDirectMessage,
                        onSendDirectVoice = { conn, voice, caption, onSent ->
                            onSendDirectVoiceMessage(conn, account, voice, caption, onSent)
                        },
                        onOpenCreateGroup = { showCreateRoomDialog = true },
                        onShowInfoMessage = onShowInfoMessage,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else {
                    // Rooms Selector Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    val rooms = (roomsState as? UiState.Success)?.data.orEmpty()
                    rooms.forEach { room ->
                        FilterChip(
                            selected = room.id == selectedRoomId,
                            onClick = { onSelectRoom(room.id) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Forum,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text(room.name) },
                            modifier = Modifier.testTag("study_room_chip_${room.id}"),
                        )
                    }

                    AssistChip(
                        onClick = { showCreateRoomDialog = true },
                        leadingIcon = {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(stringResource(R.string.chat_create_room)) },
                        modifier = Modifier.testTag("create_study_room_chip"),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Messages Feed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when (messagesState) {
                        UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = messagesState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        is UiState.Success -> {
                            if (messages.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Forum,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp),
                                        )
                                        Text(
                                            text = "ចាប់ផ្តើមសួរឬជជែកអំពីមេរៀននៅទីនេះ!",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = "អ្នកអាចភ្ជាប់ឯកសារមេរៀនពីឃ្លាំងរបស់អ្នកមកសួរបានយ៉ាងងាយ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("study_chat_messages_list"),
                                ) {
                                    items(messages, key = { it.id.ifBlank { it.hashCode() } }) { msg ->
                                        ChatMessageBubble(
                                            message = msg,
                                            isOwnMessage = msg.senderId == currentUserId,
                                            voicePlayer = roomVoicePlayer,
                                            onDelete = {
                                                if (msg.id.isNotBlank()) onDeleteMessage(msg.id)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pending Lesson Attachment Preview
                if (pendingAttachment != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column {
                                    Text(
                                        text = "${pendingAttachment.typeLabel} • ${pendingAttachment.subjectName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = pendingAttachment.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(onClick = onClearAttachment) {
                                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel))
                            }
                        }
                    }
                }

                // Message Composer (Text + Attach + Voice Recording)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (roomVoiceRecorder.isRecording) {
                        LiveVoiceRecordingBar(
                            elapsedSeconds = roomRecordingSeconds,
                            onCancel = { roomVoiceRecorder.cancel() },
                            onSend = {
                                val draft = roomVoiceRecorder.stopAndBuildDraft()
                                if (draft != null && selectedRoomId != null) {
                                    onSendVoiceMessage(account, draft, draftMessage) {
                                        draftMessage = ""
                                    }
                                } else {
                                    onShowInfoMessage("សារសំឡេងខ្លីពេក ឬមិនអាចថតបានទេ")
                                }
                            },
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = { showLessonPickerSheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("attach_lesson_chat_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = stringResource(R.string.chat_attach_lesson),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }

                            OutlinedTextField(
                                value = draftMessage,
                                onValueChange = { draftMessage = it.take(1000) },
                                placeholder = { Text(stringResource(R.string.chat_message_hint)) },
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                            )

                            VoiceRecordIconButton(
                                enabled = !isSendingMessage && selectedRoomId != null,
                                onStartRecording = {
                                    if (!roomVoiceRecorder.startRecording()) {
                                        onShowInfoMessage("មិនអាចបើកមីក្រូហ្វូនសម្រាប់ថតសំឡេងបានទេ")
                                    }
                                },
                                onPermissionDenied = { onShowInfoMessage(permissionDeniedMsg) },
                                testTag = "record_voice_chat_button",
                            )

                            FilledIconButton(
                                onClick = {
                                    onSendMessage(account, draftMessage) {
                                        draftMessage = ""
                                    }
                                },
                                enabled = !isSendingMessage && draftMessage.isNotBlank() && selectedRoomId != null,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("send_chat_message_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.chat_send_message),
                                )
                            }
                        }
                    }
                }
                }
            }

            if (showEditAccountDialog) {
                EditStudentAccountDialog(
                    account = account,
                    isSaving = isSavingAccount,
                    onDismiss = { showEditAccountDialog = false },
                    onSave = { name, grade, newPass ->
                        onSaveAccount(name, grade, newPass, account)
                        showEditAccountDialog = false
                    },
                    onSignOut = {
                        showEditAccountDialog = false
                        onSignOut()
                    },
                )
            }

            if (showCreateRoomDialog) {
                CreateStudyRoomDialog(
                    onDismiss = { showCreateRoomDialog = false },
                    onCreate = { name, desc, tag ->
                        onCreateRoom(name, desc, tag)
                        selectedChatMode = 0
                        showCreateRoomDialog = false
                    },
                )
            }

            if (showLessonPickerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLessonPickerSheet = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .detectGlobalUiTapSound()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "ជ្រើសរើសមេរៀនដើម្បីសួរក្នុងជជែក",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (localLessons.isEmpty()) {
                            Text(
                                text = "មិនទាន់មានមេរៀនក្នុងឃ្លាំងនៅឡើយទេ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 18.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(localLessons, key = { it.id }) { lesson ->
                                    val subj = subjectMap[lesson.subjectId]
                                    Card(
                                        onClick = {
                                            onAttachLesson(lesson, subj)
                                            showLessonPickerSheet = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(
                                                imageVector = lessonTypeIcon(lesson.type),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = lesson.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = "${lessonTypeLabel(lesson.type)} • ${subj?.name ?: "ទូទៅ"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    voicePlayer: VoicePlaybackController,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isOwnMessage) 18.dp else 4.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 18.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isOwnMessage) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
            border = if (isOwnMessage) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = message.senderGrade,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }

                    if (isOwnMessage) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                if (!message.attachedLessonTitle.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "${message.attachedLessonType ?: "មេរៀន"} • ${message.attachedLessonSubject ?: "ទូទៅ"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = message.attachedLessonTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!message.attachedLessonNote.isNullOrBlank()) {
                                Text(
                                    text = message.attachedLessonNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (!message.voiceAudioBase64.isNullOrBlank()) {
                    VoiceMessagePlayerCard(
                        messageId = message.id.ifBlank { message.hashCode().toString() },
                        voiceBase64 = message.voiceAudioBase64,
                        durationSec = message.voiceDurationSec ?: 1,
                        voicePlayer = voicePlayer,
                        isOwnMessage = isOwnMessage,
                    )
                }

                val isPureVoicePlaceholder = !message.voiceAudioBase64.isNullOrBlank() &&
                    message.text.startsWith(VOICE_DEFAULT_LABEL_PREFIX)
                if (!isPureVoicePlaceholder && message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                val timeMillis = message.createdAt?.toDate()?.time
                if (timeMillis != null) {
                    Text(
                        text = formatFullDateTime(timeMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateStudyRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, subjectTag: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subjectTag by remember { mutableStateOf("ទូទៅ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.detectGlobalUiTapSound(),
        title = { Text(stringResource(R.string.chat_create_room)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(stringResource(R.string.chat_room_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subjectTag,
                    onValueChange = { subjectTag = it.take(30) },
                    label = { Text("មុខវិជ្ជា / ប្រភេទ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(160) },
                    label = { Text(stringResource(R.string.chat_room_desc_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description, subjectTag) },
                enabled = name.trim().length >= 2,
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
