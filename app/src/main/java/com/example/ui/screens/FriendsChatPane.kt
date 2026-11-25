package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.cloud.DirectLessonMessage
import com.example.data.cloud.FriendConnection
import com.example.data.cloud.LessonAttachmentDraft
import com.example.data.cloud.StudentProfile
import com.example.data.cloud.UserAccount
import com.example.data.cloud.VOICE_DEFAULT_LABEL_PREFIX
import com.example.data.cloud.VoiceAttachmentDraft
import com.example.data.cloud.canonicalPairId
import com.example.data.cloud.studentCodeForUid
import com.example.ui.UiState
import com.example.ui.audio.LiveVoiceRecordingBar
import com.example.ui.audio.VoiceMessagePlayerCard
import com.example.ui.audio.VoicePlaybackController
import com.example.ui.audio.VoiceRecorderController
import com.example.ui.audio.VoiceRecordIconButton
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.audio.rememberVoicePlaybackController
import com.example.ui.components.formatFullDateTime
import kotlinx.coroutines.delay

@Composable
fun FriendsChatPane(
    currentUserId: String,
    myAccount: UserAccount,
    directoryState: UiState<List<StudentProfile>>,
    connectionsState: UiState<List<FriendConnection>>,
    directMessagesState: UiState<List<DirectLessonMessage>>,
    selectedConnectionId: String?,
    isSendingMessage: Boolean,
    pendingAttachment: LessonAttachmentDraft?,
    onSelectConnection: (String?) -> Unit,
    onSendFriendRequest: (StudentProfile, FriendConnection?) -> Unit,
    onAcceptRequest: (FriendConnection) -> Unit,
    onDeclineRequest: (FriendConnection) -> Unit,
    onRemoveConnection: (String) -> Unit,
    onOpenLessonPicker: () -> Unit,
    onClearAttachment: () -> Unit,
    onSendDirectMessage: (FriendConnection, String, () -> Unit) -> Unit,
    onDeleteDirectMessage: (String, String) -> Unit,
    onSendDirectVoice: (FriendConnection, VoiceAttachmentDraft, String, () -> Unit) -> Unit = { _, _, _, _ -> },
    onOpenCreateGroup: () -> Unit = {},
    onShowInfoMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val connections = (connectionsState as? UiState.Success)?.data.orEmpty()
    val selectedConnection = remember(connections, selectedConnectionId) {
        connections.firstOrNull { it.id == selectedConnectionId && it.status == "accepted" }
    }

    if (selectedConnection != null) {
        BackHandler { onSelectConnection(null) }
        DirectFriendChatView(
            currentUserId = currentUserId,
            connection = selectedConnection,
            messagesState = directMessagesState,
            isSendingMessage = isSendingMessage,
            pendingAttachment = pendingAttachment,
            onBack = { onSelectConnection(null) },
            onUnfriend = { onRemoveConnection(selectedConnection.id) },
            onOpenLessonPicker = onOpenLessonPicker,
            onClearAttachment = onClearAttachment,
            onSendMessage = { text, onSent ->
                onSendDirectMessage(selectedConnection, text, onSent)
            },
            onSendVoice = { voice, caption, onSent ->
                onSendDirectVoice(selectedConnection, voice, caption, onSent)
            },
            onShowInfoMessage = onShowInfoMessage,
            onDeleteMessage = { msgId ->
                onDeleteDirectMessage(selectedConnection.id, msgId)
            },
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val myStudentCode = remember(currentUserId) { studentCodeForUid(currentUserId) }
    var searchQuery by remember { mutableStateOf("") }

    val incomingRequests = remember(connections, currentUserId) {
        connections.filter { it.status == "pending" && it.recipientId == currentUserId }
    }
    val outgoingRequests = remember(connections, currentUserId) {
        connections.filter { it.status == "pending" && it.requesterId == currentUserId }
    }
    val acceptedFriends = remember(connections) {
        connections.filter { it.status == "accepted" }
    }
    val connectionByPeerId = remember(connections, currentUserId) {
        connections.associateBy { it.peerUserId(currentUserId) }
    }

    val allStudents = (directoryState as? UiState.Success)?.data.orEmpty()
    val filteredStudents = remember(allStudents, searchQuery, currentUserId) {
        val q = searchQuery.trim().lowercase()
        allStudents.filter { student ->
            student.userId != currentUserId && (
                q.isEmpty() ||
                    student.displayName.lowercase().contains(q) ||
                    student.gradeLevel.lowercase().contains(q) ||
                    student.studentCode.lowercase().contains(q)
                )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("friends_overview_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. My Student Code Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "លេខកូដសិស្សរបស់អ្នក៖ $myStudentCode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("my_student_code_text"),
                        )
                        Text(
                            text = "ចែករំលែកឈ្មោះ ឬលេខកូដនេះដើម្បីឱ្យមិត្តភក្តិរកឃើញអ្នក",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Student Code", myStudentCode))
                            Toast.makeText(context, "បានចម្លងលេខកូដ $myStudentCode", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "ចម្លងលេខកូដ",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // 2. Search Bar for finding classmates
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it.take(40) },
                placeholder = { Text(stringResource(R.string.friends_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("friends_search_input"),
            )
        }

        // 3. Incoming Friend Requests
        if (incomingRequests.isNotEmpty()) {
            item {
                Text(
                    text = "${stringResource(R.string.friends_requests_title)} (${incomingRequests.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(incomingRequests, key = { "in_${it.id}" }) { req ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PeerAvatar(name = req.requesterName)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.requesterName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${req.requesterGrade} • ចង់ធ្វើជាមិត្តភក្តិជាមួយអ្នក",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { onAcceptRequest(req) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("accept_friend_${req.id}"),
                            ) {
                                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.friends_accept))
                            }
                            OutlinedButton(
                                onClick = { onDeclineRequest(req) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("decline_friend_${req.id}"),
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.friends_decline))
                            }
                        }
                    }
                }
            }
        }

        // 4. My Accepted Friends (1-on-1 Lesson Chat) & Create Study Group shortcut
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${stringResource(R.string.friends_list_title)} (${acceptedFriends.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                FilledTonalButton(
                    onClick = onOpenCreateGroup,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("friends_create_group_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.friends_create_group_button))
                }
            }
        }

        if (connectionsState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        } else if (acceptedFriends.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "មិនទាន់មានមិត្តភក្តិនៅឡើយទេ។ សូមផ្ញើសំណើ Add Friend ទៅសិស្សខាងក្រោម!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        } else {
            items(acceptedFriends, key = { "friend_${it.id}" }) { conn ->
                val peerName = conn.peerName(currentUserId)
                val peerGrade = conn.peerGrade(currentUserId)
                Card(
                    onClick = { onSelectConnection(conn.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_item_${conn.id}"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PeerAvatar(name = peerName)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = peerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = peerGrade,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                            Text(
                                text = conn.lastMessageText?.takeIf { it.isNotBlank() }
                                    ?: "ចុចដើម្បីជជែក និងសួរមេរៀនជាមួយគ្នា",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = "ជជែក",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // 5. Outgoing Sent Requests
        if (outgoingRequests.isNotEmpty()) {
            item {
                Text(
                    text = "សំណើដែលបានផ្ញើ (${outgoingRequests.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(outgoingRequests, key = { "out_${it.id}" }) { outReq ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            PeerAvatar(name = outReq.recipientName)
                            Column {
                                Text(
                                    text = outReq.recipientName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${outReq.recipientGrade} • កំពុងរង់ចាំការយល់ព្រម",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(
                            onClick = { onRemoveConnection(outReq.id) },
                            modifier = Modifier.testTag("cancel_friend_${outReq.id}"),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }

        // 6. Discover Other Students to Add Friend
        item {
            Text(
                text = stringResource(R.string.friends_discover_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        when (directoryState) {
            UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Text(
                        text = directoryState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is UiState.Success -> {
                if (filteredStudents.isEmpty()) {
                    item {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "មិនទាន់មានសិស្សផ្សេងទៀតចុះឈ្មោះនៅឡើយទេ។"
                            } else {
                                "រកមិនឃើញសិស្សដែលត្រូវនឹង «$searchQuery» ទេ។"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                } else {
                    items(filteredStudents, key = { "peer_${it.userId}" }) { student ->
                        val existing = connectionByPeerId[student.userId]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                PeerAvatar(name = student.displayName)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${student.gradeLevel} • កូដ ${student.studentCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                when {
                                    existing?.status == "accepted" -> {
                                        FilledTonalButton(
                                            onClick = { onSelectConnection(existing.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("chat_friend_${student.userId}"),
                                        ) {
                                            Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ជជែក")
                                        }
                                    }

                                    existing?.status == "pending" && existing.recipientId == currentUserId -> {
                                        Button(
                                            onClick = { onAcceptRequest(existing) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("accept_friend_${existing.id}"),
                                        ) {
                                            Text(stringResource(R.string.friends_accept))
                                        }
                                    }

                                    existing?.status == "pending" -> {
                                        OutlinedButton(
                                            onClick = { onRemoveConnection(existing.id) },
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Text(stringResource(R.string.friends_pending_sent))
                                        }
                                    }

                                    else -> {
                                        FilledTonalButton(
                                            onClick = {
                                                val fallbackId = canonicalPairId(myAccount.userId, student.userId)
                                                val targetConn = existing?.takeIf { it.id == fallbackId }
                                                onSendFriendRequest(student, targetConn)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("add_friend_${student.userId}"),
                                        ) {
                                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.friends_add_button))
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
private fun DirectFriendChatView(
    currentUserId: String,
    connection: FriendConnection,
    messagesState: UiState<List<DirectLessonMessage>>,
    isSendingMessage: Boolean,
    pendingAttachment: LessonAttachmentDraft?,
    onBack: () -> Unit,
    onUnfriend: () -> Unit,
    onOpenLessonPicker: () -> Unit,
    onClearAttachment: () -> Unit,
    onSendMessage: (String, () -> Unit) -> Unit,
    onSendVoice: (VoiceAttachmentDraft, String, () -> Unit) -> Unit,
    onShowInfoMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val peerName = connection.peerName(currentUserId)
    val peerGrade = connection.peerGrade(currentUserId)
    var draftMessage by remember(connection.id) { mutableStateOf("") }
    var showUnfriendConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()

    val voicePlayer = rememberVoicePlaybackController()
    val voiceRecorder = remember(context) { VoiceRecorderController(context) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    val permissionDeniedMsg = stringResource(R.string.voice_permission_denied)

    DisposableEffect(voiceRecorder) {
        onDispose { voiceRecorder.release() }
    }

    LaunchedEffect(voiceRecorder.isRecording) {
        if (voiceRecorder.isRecording) {
            recordingSeconds = 0
            while (voiceRecorder.isRecording && recordingSeconds < 45) {
                delay(1000L)
                recordingSeconds++
            }
            if (voiceRecorder.isRecording) {
                val draft = voiceRecorder.stopAndBuildDraft()
                if (draft != null) {
                    onSendVoice(draft, draftMessage) { draftMessage = "" }
                }
            }
        } else {
            recordingSeconds = 0
        }
    }

    LaunchedEffect(messages.size, connection.id) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Direct Chat Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("friend_chat_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                    PeerAvatar(name = peerName)
                    Column {
                        Text(
                            text = peerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "$peerGrade • មិត្តភក្តិ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                IconButton(
                    onClick = { showUnfriendConfirm = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("unfriend_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonRemove,
                        contentDescription = stringResource(R.string.friends_remove),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Direct Messages Feed
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
                                    text = "ជជែកសួរអំពីមេរៀនជាមួយ $peerName",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "អ្នកអាចភ្ជាប់ឯកសារមេរៀនរបស់អ្នកផ្ញើសួរគ្នាបានយ៉ាងងាយ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("direct_chat_messages_list"),
                        ) {
                            items(messages, key = { it.id.ifBlank { it.hashCode() } }) { msg ->
                                DirectMessageBubble(
                                    message = msg,
                                    isOwnMessage = msg.senderId == currentUserId,
                                    voicePlayer = voicePlayer,
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

        // Composer Bar (Text + Lesson Attach + Voice Recording)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (voiceRecorder.isRecording) {
                LiveVoiceRecordingBar(
                    elapsedSeconds = recordingSeconds,
                    onCancel = { voiceRecorder.cancel() },
                    onSend = {
                        val draft = voiceRecorder.stopAndBuildDraft()
                        if (draft != null) {
                            onSendVoice(draft, draftMessage) { draftMessage = "" }
                        } else {
                            onShowInfoMessage("សារសំឡេងខ្លីពេក ឬមិនអាចថតបានទេ")
                        }
                    },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onOpenLessonPicker,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("attach_lesson_direct_button"),
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
                        placeholder = { Text(stringResource(R.string.friends_chat_hint)) },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("direct_chat_message_input"),
                    )

                    VoiceRecordIconButton(
                        enabled = !isSendingMessage,
                        onStartRecording = {
                            if (!voiceRecorder.startRecording()) {
                                onShowInfoMessage("មិនអាចបើកមីក្រូហ្វូនសម្រាប់ថតសំឡេងបានទេ")
                            }
                        },
                        onPermissionDenied = { onShowInfoMessage(permissionDeniedMsg) },
                        testTag = "record_voice_direct_button",
                    )

                    FilledIconButton(
                        onClick = {
                            onSendMessage(draftMessage) {
                                draftMessage = ""
                            }
                        },
                        enabled = !isSendingMessage && draftMessage.isNotBlank(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("send_direct_message_button"),
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

    if (showUnfriendConfirm) {
        AlertDialog(
            onDismissRequest = { showUnfriendConfirm = false },
            modifier = Modifier.detectGlobalUiTapSound(),
            title = { Text(stringResource(R.string.friends_remove)) },
            text = { Text("តើអ្នកប្រាកដថាចង់ដក $peerName ចេញពីបញ្ជីមិត្តភក្តិមែនទេ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnfriendConfirm = false
                        onUnfriend()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnfriendConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DirectMessageBubble(
    message: DirectLessonMessage,
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
                    Text(
                        text = "${message.senderName} • ${message.senderGrade}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
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
private fun PeerAvatar(name: String) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
