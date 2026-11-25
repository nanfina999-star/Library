package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.ChatMessage
import com.example.data.cloud.DirectLessonMessage
import com.example.data.cloud.FriendConnection
import com.example.data.cloud.LessonAttachmentDraft
import com.example.data.cloud.StudentProfile
import com.example.data.cloud.StudyCloudRepository
import com.example.data.cloud.StudyRoom
import com.example.data.cloud.UserAccount
import com.example.data.cloud.VoiceAttachmentDraft
import com.example.data.local.LessonEntity
import com.example.data.local.SubjectEntity
import com.example.ui.components.lessonTypeLabel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class StudyChatViewModel(
    private val repository: StudyCloudRepository,
    private val currentUserId: String,
) : ViewModel() {

    private companion object {
        const val TAG = "StudyChatVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: StateFlow<String?> = _selectedRoomId.asStateFlow()

    private val _selectedFriendConnectionId = MutableStateFlow<String?>(null)
    val selectedFriendConnectionId: StateFlow<String?> = _selectedFriendConnectionId.asStateFlow()

    private val _isAccountLocked = MutableStateFlow(false)
    val isAccountLocked: StateFlow<Boolean> = _isAccountLocked.asStateFlow()

    private val _isSavingAccount = MutableStateFlow(false)
    val isSavingAccount: StateFlow<Boolean> = _isSavingAccount.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<LessonAttachmentDraft?>(null)
    val pendingAttachment: StateFlow<LessonAttachmentDraft?> = _pendingAttachment.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var directorySyncedForUid = false

    val accountState: StateFlow<UiState<UserAccount?>> = repository.observeUserAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing student account", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "មិនអាចផ្ទុកគណនីបានទេ"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val roomsState: StateFlow<UiState<List<StudyRoom>>> = repository.observeStudyRooms()
        .onEach { rooms ->
            if (rooms.isEmpty()) {
                repository.ensureDefaultStudyRooms()
            } else if (_selectedRoomId.value == null || rooms.none { it.id == _selectedRoomId.value }) {
                _selectedRoomId.value = rooms.first().id
            }
        }
        .map<List<StudyRoom>, UiState<List<StudyRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing study rooms", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "មិនអាចផ្ទុកបន្ទប់ជជែកបានទេ"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesState: StateFlow<UiState<List<ChatMessage>>> = _selectedRoomId
        .flatMapLatest { roomId ->
            if (roomId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeRoomMessages(roomId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing messages", rootError)
                        emit(UiState.Error(rootError.localizedMessage ?: "មិនអាចផ្ទុកសារជជែកបានទេ"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val studentDirectoryState: StateFlow<UiState<List<StudentProfile>>> = repository.observeStudentDirectory()
        .onEach { list ->
            if (!directorySyncedForUid && list.none { it.userId == currentUserId }) {
                val acc = (accountState.value as? UiState.Success)?.data
                if (acc != null) {
                    directorySyncedForUid = true
                    repository.syncStudentDirectoryProfile(acc)
                }
            }
        }
        .map<List<StudentProfile>, UiState<List<StudentProfile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing student directory", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "មិនអាចផ្ទុកបញ្ជីសិស្សបានទេ"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val friendConnectionsState: StateFlow<UiState<List<FriendConnection>>> = repository.observeFriendConnections(currentUserId)
        .map<List<FriendConnection>, UiState<List<FriendConnection>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing friend connections", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "មិនអាចផ្ទុកបញ្ជីមិត្តភក្តិបានទេ"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val directMessagesState: StateFlow<UiState<List<DirectLessonMessage>>> = _selectedFriendConnectionId
        .flatMapLatest { connectionId ->
            if (connectionId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeDirectMessages(connectionId, currentUserId)
                    .map<List<DirectLessonMessage>, UiState<List<DirectLessonMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing direct messages", rootError)
                        emit(UiState.Error(rootError.localizedMessage ?: "មិនអាចផ្ទុកសារជជែកជាមួយមិត្តបានទេ"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    fun selectRoom(roomId: String) {
        _selectedRoomId.value = roomId
    }

    fun selectFriendConnection(connectionId: String?) {
        _selectedFriendConnectionId.value = connectionId
    }

    fun attachLesson(lesson: LessonEntity, subject: SubjectEntity?) {
        val noteExcerpt = buildString {
            if (lesson.description.isNotBlank()) {
                append(lesson.description.trim())
            }
            if (lesson.noteContent.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(lesson.noteContent.trim())
            }
        }.take(450)
        _pendingAttachment.value = LessonAttachmentDraft(
            title = lesson.title.take(120),
            typeLabel = lessonTypeLabel(lesson.type),
            subjectName = subject?.name ?: "ទូទៅ",
            noteExcerpt = noteExcerpt,
        )
    }

    fun clearPendingAttachment() {
        _pendingAttachment.value = null
    }

    fun lockAccount() {
        _isAccountLocked.value = true
    }

    fun unlockWithPassword(account: UserAccount, password: String): Boolean {
        val verified = repository.verifyUserPasscode(account, password)
        if (verified) {
            _isAccountLocked.value = false
            _events.tryEmit("បានដោះសោគណនីដោយជោគជ័យ")
        } else {
            _events.tryEmit("ពាក្យសម្ងាត់មិនត្រឹមត្រូវទេ")
        }
        return verified
    }

    fun saveStudentAccount(
        displayName: String,
        gradeLevel: String,
        password: String,
        existingAccount: UserAccount?,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSavingAccount.value = true
            val result = repository.saveUserAccount(
                userId = currentUserId,
                displayName = displayName,
                gradeLevel = gradeLevel,
                password = password,
                existingAccount = existingAccount,
            )
            _isSavingAccount.value = false
            result.onSuccess {
                _isAccountLocked.value = false
                _events.tryEmit(
                    if (existingAccount == null) "បានបង្កើតគណនីសិស្សរួចរាល់"
                    else "បានកែប្រែព័ត៌មានគណនីរួចរាល់",
                )
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "មិនអាចរក្សាទុកគណនីបានទេ")
            }
        }
    }

    fun createStudyRoom(
        name: String,
        description: String,
        subjectTag: String,
        onCreated: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = repository.createStudyRoom(name, description, subjectTag)
            result.onSuccess { roomId ->
                _selectedRoomId.value = roomId
                _events.tryEmit("បានបង្កើតបន្ទប់ជជែកថ្មី")
                onCreated()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "មិនអាចបង្កើតបន្ទប់ជជែកបានទេ")
            }
        }
    }

    fun sendMessage(
        account: UserAccount,
        text: String,
        voice: VoiceAttachmentDraft? = null,
        onSent: () -> Unit = {},
    ) {
        val roomId = _selectedRoomId.value ?: return
        viewModelScope.launch {
            _isSendingMessage.value = true
            val attachment = _pendingAttachment.value
            val result = repository.sendMessage(
                roomId = roomId,
                account = account,
                text = text,
                attachment = attachment,
                voice = voice,
            )
            _isSendingMessage.value = false
            result.onSuccess {
                _pendingAttachment.value = null
                onSent()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "មិនអាចផ្ញើសារបានទេ")
            }
        }
    }

    fun emitInfo(message: String) {
        _events.tryEmit(message)
    }

    fun deleteMessage(messageId: String) {
        val roomId = _selectedRoomId.value ?: return
        viewModelScope.launch {
            repository.deleteMessage(roomId, messageId).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "មិនអាចលុបសារបានទេ")
            }
        }
    }

    fun sendFriendRequest(
        myAccount: UserAccount,
        targetStudent: StudentProfile,
        existingConnection: FriendConnection?,
    ) {
        viewModelScope.launch {
            repository.sendFriendRequest(myAccount, targetStudent, existingConnection)
                .onSuccess {
                    _events.tryEmit("បានផ្ញើសំណើសុំធ្វើមិត្តទៅ ${targetStudent.displayName}")
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "មិនអាចផ្ញើសំណើសុំធ្វើមិត្តបានទេ")
                }
        }
    }

    fun respondToFriendRequest(
        connection: FriendConnection,
        myAccount: UserAccount,
        accept: Boolean,
    ) {
        viewModelScope.launch {
            repository.respondToFriendRequest(connection.id, myAccount, accept)
                .onSuccess {
                    val peerName = connection.peerName(currentUserId)
                    _events.tryEmit(
                        if (accept) "បានក្លាយជាមិត្តភក្តិជាមួយ $peerName រួចរាល់"
                        else "បានបដិសេធសំណើពី $peerName",
                    )
                    if (accept) {
                        _selectedFriendConnectionId.value = connection.id
                    }
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "មិនអាចធ្វើបច្ចុប្បន្នភាពសំណើបានទេ")
                }
        }
    }

    fun removeFriendConnection(connectionId: String) {
        viewModelScope.launch {
            repository.removeFriendConnection(connectionId)
                .onSuccess {
                    if (_selectedFriendConnectionId.value == connectionId) {
                        _selectedFriendConnectionId.value = null
                    }
                    _events.tryEmit("បានលុបទំនាក់ទំនងមិត្តភក្តិ")
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "មិនអាចដកមិត្តភក្តិបានទេ")
                }
        }
    }

    fun sendDirectMessage(
        connection: FriendConnection,
        account: UserAccount,
        text: String,
        voice: VoiceAttachmentDraft? = null,
        onSent: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSendingMessage.value = true
            val attachment = _pendingAttachment.value
            val result = repository.sendDirectMessage(
                connection = connection,
                account = account,
                text = text,
                attachment = attachment,
                voice = voice,
            )
            _isSendingMessage.value = false
            result.onSuccess {
                _pendingAttachment.value = null
                onSent()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "មិនអាចផ្ញើសារទៅមិត្តភក្តិបានទេ")
            }
        }
    }

    fun deleteDirectMessage(connectionId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteDirectMessage(connectionId, messageId).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "មិនអាចលុបសារបានទេ")
            }
        }
    }
}
