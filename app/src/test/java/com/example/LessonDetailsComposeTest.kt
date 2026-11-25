package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.cloud.ChatMessage
import com.example.data.cloud.DirectLessonMessage
import com.example.data.cloud.FriendConnection
import com.example.data.cloud.LessonAttachmentDraft
import com.example.data.cloud.StudentProfile
import com.example.data.cloud.StudyRoom
import com.example.data.cloud.UserAccount
import com.example.data.local.LessonEntity
import com.example.data.local.LessonType
import com.example.data.local.SubjectEntity
import com.example.ui.LessonSortOrder
import com.example.ui.UiState
import com.example.ui.VaultStats
import com.example.ui.audio.LocalUiSoundEnabled
import com.example.ui.audio.LocalUiSoundPlayer
import com.example.ui.audio.SoundToggleIconButton
import com.example.ui.audio.UiSoundPlayer
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.components.QuickCategorizeDialog
import com.example.ui.components.StudentAccountFormCard
import com.example.ui.components.formatByteSize
import com.example.ui.components.formatFullDateTime
import com.example.ui.screens.LessonViewerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.StudyChatScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.theme.LessonVaultTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LessonDetailsComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `document detailed view shows creation date, file size, and large notes preview`() {
        val createdTime = 1_735_689_600_000L
        val sampleLesson = LessonEntity(
            id = 42L,
            title = "សង្ខេបប្រវត្តិសាស្ត្រខ្មែរ",
            description = "ឯកសារសិក្សា",
            subjectId = 1L,
            type = LessonType.DOCUMENT,
            internalFilePath = "history.docx",
            originalFileName = "khmer_history_summary.docx",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            fileSizeBytes = 256 * 1024L,
            noteContent = "ចំណុចសំខាន់ទី១៖ សម័យអង្គរ\nចំណុចសំខាន់ទី២៖ វប្បធម៌ និងស្ថាបត្យកម្ម",
            labels = "សង្ខេប, ត្រៀមប្រឡង",
            createdAt = createdTime,
            updatedAt = createdTime + 3_600_000L,
        )
        val sampleSubject = SubjectEntity(
            id = 1L,
            name = "ប្រវត្តិសាស្ត្រ",
            colorHex = 0xFFB55D28,
            iconKey = "history",
        )

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                LessonViewerScreen(
                    lesson = sampleLesson,
                    subject = sampleSubject,
                    isDarkTheme = false,
                    onToggleTheme = {},
                    resolveFile = { null },
                    getShareUri = { null },
                    readTextFile = { "" },
                    onBack = {},
                    onTogglePin = {},
                    onCategorize = {},
                    onEditNoteLesson = {},
                    onEditMetadata = {},
                    onUpdateLastPage = { _, _ -> },
                    onSaveStudyNotes = { _, _ -> },
                    onDeleteLesson = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("viewer_tab_details").performClick()

        composeTestRule.onNodeWithTag("metadata_created_at")
            .assertTextContains(formatFullDateTime(createdTime))
        composeTestRule.onNodeWithTag("metadata_file_size")
            .assertTextContains(formatByteSize(256 * 1024L))
        composeTestRule.onNodeWithTag("large_notes_preview_text")
            .assertTextContains("សម័យអង្គរ", substring = true)
    }

    @Test
    fun `main screen sort menu allows selecting date created, name, and category`() {
        var selectedOrder by mutableStateOf(LessonSortOrder.DATE_CREATED)
        val subject = SubjectEntity(id = 1L, name = "គណិតវិទ្យា", colorHex = 0xFFD97706, iconKey = "math")
        val lesson = LessonEntity(
            id = 1L,
            subjectId = 1L,
            title = "អាំងតេក្រាល",
            type = LessonType.NOTE,
            noteContent = "Sample",
        )

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                LibraryScreen(
                    lessons = listOf(lesson),
                    allLessonsCount = 1,
                    subjects = listOf(subject),
                    categoryCounts = mapOf(1L to 1),
                    availableLabels = emptyList(),
                    stats = VaultStats(totalLessons = 1, totalSubjects = 1),
                    searchQuery = "",
                    selectedType = null,
                    selectedSubjectId = null,
                    selectedLabel = null,
                    sortOrder = selectedOrder,
                    onSearchChange = {},
                    onSelectType = {},
                    onSelectSubject = {},
                    onSelectLabel = {},
                    onAddCategory = {},
                    onSelectSortOrder = { selectedOrder = it },
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

        composeTestRule.onNodeWithTag("sort_lessons_chip").performClick()
        composeTestRule.onNodeWithTag("sort_option_date_created").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sort_option_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sort_option_category").assertIsDisplayed()

        composeTestRule.onNodeWithTag("sort_option_name").performClick()
        assertEquals(LessonSortOrder.NAME, selectedOrder)

        composeTestRule.onNodeWithTag("sort_lessons_chip").performClick()
        composeTestRule.onNodeWithTag("sort_option_category").performClick()
        assertEquals(LessonSortOrder.CATEGORY, selectedOrder)
    }

    @Test
    fun `student account creation form captures name, grade level, and password`() {
        var savedName = ""
        var savedGrade = ""
        var savedPassword = ""

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                StudentAccountFormCard(
                    existingAccount = null,
                    suggestedName = "",
                    isSaving = false,
                    onSave = { name, grade, password ->
                        savedName = name
                        savedGrade = grade
                        savedPassword = password
                    },
                    onSignOut = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("account_name_input").performScrollTo().performTextInput("ដារ៉ា")
        composeTestRule.onNodeWithTag("account_password_input").performScrollTo().performTextInput("pass2026")
        composeTestRule.onNodeWithTag("account_submit_button").performScrollTo().assertIsEnabled().performClick()

        assertEquals("ដារ៉ា", savedName)
        assertEquals("ថ្នាក់ទី ១២", savedGrade)
        assertEquals("pass2026", savedPassword)
    }

    @Test
    fun `study chat screen shows rooms, attached lesson discussion, and sends messages`() {
        var sentText = ""
        val account = UserAccount(
            id = "u1",
            userId = "u1",
            displayName = "បុប្ផា",
            gradeLevel = "ថ្នាក់ទី ១១",
        )
        val room = StudyRoom(
            id = "general_study",
            creatorId = "u1",
            name = "បន្ទប់ជជែករួម",
            description = "សួរមេរៀនទូទៅ",
            subjectTag = "ទូទៅ",
        )
        val existingMessage = ChatMessage(
            id = "m1",
            roomId = "general_study",
            senderId = "u2",
            senderName = "វិចិត្រ",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "តើរូបមន្តអាំងតេក្រាលនេះប្រើយ៉ាងម៉េច?",
            attachedLessonTitle = "អាំងតេក្រាលមូលដ្ឋាន",
            attachedLessonType = "កំណត់ត្រា",
            attachedLessonSubject = "គណិតវិទ្យា",
        )

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                StudyChatScreen(
                    currentUserId = "u1",
                    suggestedName = "បុប្ផា",
                    accountState = UiState.Success(account),
                    roomsState = UiState.Success(listOf(room)),
                    messagesState = UiState.Success(listOf(existingMessage)),
                    selectedRoomId = "general_study",
                    isAccountLocked = false,
                    isSavingAccount = false,
                    isSendingMessage = false,
                    pendingAttachment = LessonAttachmentDraft(
                        title = "ប្រវត្តិសាស្ត្រអង្គរ",
                        typeLabel = "PDF",
                        subjectName = "ប្រវត្តិសាស្ត្រ",
                        noteExcerpt = "សម័យអង្គរ",
                    ),
                    localLessons = emptyList(),
                    localSubjects = emptyList(),
                    onSaveAccount = { _, _, _, _ -> },
                    onUnlockAccount = { _, _ -> true },
                    onLockAccount = {},
                    onSignOut = {},
                    onSelectRoom = {},
                    onCreateRoom = { _, _, _ -> },
                    onAttachLesson = { _, _ -> },
                    onClearAttachment = {},
                    onSendMessage = { _, text, onSent ->
                        sentText = text
                        onSent()
                    },
                    onDeleteMessage = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("study_room_chip_general_study").assertIsDisplayed()
        composeTestRule.onNodeWithText("អាំងតេក្រាលមូលដ្ឋាន").assertIsDisplayed()
        composeTestRule.onNodeWithText("ប្រវត្តិសាស្ត្រអង្គរ").assertIsDisplayed()

        composeTestRule.onNodeWithTag("chat_message_input").performTextInput("នេះជាចម្លើយសង្ខេប")
        composeTestRule.onNodeWithTag("send_chat_message_button").assertIsEnabled().performClick()

        assertEquals("នេះជាចម្លើយសង្ខេប", sentText)
    }

    @Test
    fun `tapping any UI element plays click sound when enabled and respects mute toggle`() {
        var clickCount by mutableIntStateOf(0)
        var soundEnabled by mutableStateOf(true)
        val fakePlayer = object : UiSoundPlayer {
            override fun playClick() {
                clickCount++
            }
        }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiSoundPlayer provides fakePlayer,
                LocalUiSoundEnabled provides soundEnabled,
            ) {
                LessonVaultTheme(darkTheme = false) {
                    Box(modifier = Modifier.detectGlobalUiTapSound()) {
                        Column {
                            Button(
                                onClick = {},
                                modifier = Modifier.testTag("sample_ui_button"),
                            ) {
                                Text("ចុចសាកល្បង")
                            }
                            SoundToggleIconButton(
                                soundEnabled = soundEnabled,
                                onToggle = { soundEnabled = !soundEnabled },
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("sample_ui_button").performClick()
        assertEquals(1, clickCount)

        // Tapping toggle while enabled plays click and disables sound
        composeTestRule.onNodeWithTag("sound_toggle_button").performClick()
        assertEquals(2, clickCount)
        assertEquals(false, soundEnabled)

        // Further UI clicks do not trigger sound while muted
        composeTestRule.onNodeWithTag("sample_ui_button").performClick()
        assertEquals(2, clickCount)
    }

    @Test
    fun `friends tab supports mutual friend requests, adding students, and direct lesson chat`() {
        var acceptedConnectionId = ""
        var requestedPeerId = ""
        var sentDirectText = ""
        var selectedConnId by mutableStateOf<String?>(null)

        val myAccount = UserAccount(
            id = "u1",
            userId = "u1",
            displayName = "សុខា",
            gradeLevel = "ថ្នាក់ទី ១២",
        )
        val incomingRequest = FriendConnection(
            id = "peer_b_u1",
            participantIds = listOf("peer_b", "u1"),
            requesterId = "peer_b",
            requesterName = "វិបុល",
            requesterGrade = "ថ្នាក់ទី ១២",
            recipientId = "u1",
            recipientName = "សុខា",
            recipientGrade = "ថ្នាក់ទី ១២",
            status = "pending",
        )
        val acceptedFriend = FriendConnection(
            id = "peer_c_u1",
            participantIds = listOf("peer_c", "u1"),
            requesterId = "u1",
            requesterName = "សុខា",
            requesterGrade = "ថ្នាក់ទី ១២",
            recipientId = "peer_c",
            recipientName = "កញ្ញា",
            recipientGrade = "ថ្នាក់ទី ១១",
            status = "accepted",
        )
        val discoverableStudent = StudentProfile(
            id = "peer_d",
            userId = "peer_d",
            displayName = "រតនា",
            gradeLevel = "ថ្នាក់ទី ១០",
            studentCode = "PEERD1",
            visibility = "students",
        )
        val directMsg = DirectLessonMessage(
            id = "dm1",
            connectionId = "peer_c_u1",
            participantIds = listOf("peer_c", "u1"),
            senderId = "peer_c",
            senderName = "កញ្ញា",
            senderGrade = "ថ្នាក់ទី ១១",
            text = "តើអាំងតេក្រាលនេះដោះស្រាយយ៉ាងម៉េច?",
            attachedLessonTitle = "អាំងតេក្រាលកំណត់",
            attachedLessonType = "កំណត់ត្រា",
            attachedLessonSubject = "គណិតវិទ្យា",
        )

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                StudyChatScreen(
                    currentUserId = "u1",
                    suggestedName = "សុខា",
                    accountState = UiState.Success(myAccount),
                    roomsState = UiState.Success(emptyList()),
                    messagesState = UiState.Success(emptyList()),
                    selectedRoomId = null,
                    isAccountLocked = false,
                    isSavingAccount = false,
                    isSendingMessage = false,
                    pendingAttachment = null,
                    localLessons = emptyList(),
                    localSubjects = emptyList(),
                    onSaveAccount = { _, _, _, _ -> },
                    onUnlockAccount = { _, _ -> true },
                    onLockAccount = {},
                    onSignOut = {},
                    onSelectRoom = {},
                    onCreateRoom = { _, _, _ -> },
                    onAttachLesson = { _, _ -> },
                    onClearAttachment = {},
                    onSendMessage = { _, _, _ -> },
                    onDeleteMessage = {},
                    studentDirectoryState = UiState.Success(listOf(discoverableStudent)),
                    friendConnectionsState = UiState.Success(listOf(incomingRequest, acceptedFriend)),
                    directMessagesState = UiState.Success(listOf(directMsg)),
                    selectedFriendConnectionId = selectedConnId,
                    onSelectFriendConnection = { selectedConnId = it },
                    onSendFriendRequest = { _, target, _ ->
                        requestedPeerId = target.userId
                    },
                    onRespondFriendRequest = { conn, _, accept ->
                        if (accept) acceptedConnectionId = conn.id
                    },
                    onSendDirectMessage = { _, _, text, onSent ->
                        sentDirectText = text
                        onSent()
                    },
                )
            }
        }

        // Switch to Friends tab
        composeTestRule.onNodeWithTag("chat_mode_friends_tab").performClick()
        composeTestRule.onNodeWithTag("friends_overview_list").assertIsDisplayed()

        // Accept incoming friend request
        composeTestRule.onNodeWithTag("friends_overview_list")
            .performScrollToNode(hasTestTag("accept_friend_peer_b_u1"))
        composeTestRule.onNodeWithTag("accept_friend_peer_b_u1").performClick()
        assertEquals("peer_b_u1", acceptedConnectionId)

        // Send a friend request to another student
        composeTestRule.onNodeWithTag("friends_overview_list")
            .performScrollToNode(hasTestTag("add_friend_peer_d"))
        composeTestRule.onNodeWithTag("add_friend_peer_d").performClick()
        assertEquals("peer_d", requestedPeerId)

        // Open 1-on-1 direct chat with accepted friend & send lesson message
        composeTestRule.onNodeWithTag("friends_overview_list")
            .performScrollToNode(hasTestTag("friend_item_peer_c_u1"))
        composeTestRule.onNodeWithTag("friend_item_peer_c_u1").performClick()

        composeTestRule.onNodeWithText("អាំងតេក្រាលកំណត់").assertIsDisplayed()
        composeTestRule.onNodeWithTag("direct_chat_message_input").performTextInput("នេះជារូបមន្តសង្ខេប!")
        composeTestRule.onNodeWithTag("send_direct_message_button").assertIsEnabled().performClick()
        assertEquals("នេះជារូបមន្តសង្ខេប!", sentDirectText)
    }

    @Test
    fun `main screen top search bar captures title query cleanly`() {
        var query by mutableStateOf("")
        val subject = SubjectEntity(id = 1L, name = "គណិតវិទ្យា", colorHex = 0xFFD97706, iconKey = "math")
        val lessons = listOf(
            LessonEntity(id = 1L, subjectId = 1L, title = "អាំងតេក្រាល", type = LessonType.NOTE),
            LessonEntity(id = 2L, subjectId = 1L, title = "ធរណីមាត្រ", type = LessonType.PDF),
        )

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                LibraryScreen(
                    lessons = lessons.filter { query.isBlank() || it.title.contains(query) },
                    allLessonsCount = lessons.size,
                    subjects = listOf(subject),
                    categoryCounts = mapOf(1L to 2),
                    availableLabels = emptyList(),
                    stats = VaultStats(totalLessons = 2, totalSubjects = 1),
                    searchQuery = query,
                    selectedType = null,
                    selectedSubjectId = null,
                    selectedLabel = null,
                    sortOrder = LessonSortOrder.DATE_CREATED,
                    onSearchChange = { query = it },
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

        composeTestRule.onNodeWithTag("library_search_input").assertIsDisplayed().performTextInput("ធរណី")
        assertEquals("ធរណី", query)
        composeTestRule.onNodeWithTag("library_search_clear_button").performClick()
        assertEquals("", query)
    }

    @Test
    fun `voice message cards, mic controls, and create group shortcut render in group and friend chats`() {
        val account = UserAccount(
            id = "u1",
            userId = "u1",
            displayName = "សុខា",
            gradeLevel = "ថ្នាក់ទី ១២",
        )
        val room = StudyRoom(
            id = "math_qa",
            creatorId = "u1",
            name = "គ្រុបគណិត",
            description = "សួរមេរៀន",
            subjectTag = "គណិតវិទ្យា",
        )
        val voiceRoomMsg = ChatMessage(
            id = "vm1",
            roomId = "math_qa",
            senderId = "u2",
            senderName = "វិចិត្រ",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "សារសំឡេង (12 វិនាទី)",
            voiceAudioBase64 = "AAAA",
            voiceDurationSec = 12,
        )
        val friendConn = FriendConnection(
            id = "peer_c_u1",
            participantIds = listOf("peer_c", "u1"),
            requesterId = "u1",
            requesterName = "សុខា",
            requesterGrade = "ថ្នាក់ទី ១២",
            recipientId = "peer_c",
            recipientName = "កញ្ញា",
            recipientGrade = "ថ្នាក់ទី ១១",
            status = "accepted",
        )
        val voiceDirectMsg = DirectLessonMessage(
            id = "dm_v1",
            connectionId = "peer_c_u1",
            participantIds = listOf("peer_c", "u1"),
            senderId = "peer_c",
            senderName = "កញ្ញា",
            senderGrade = "ថ្នាក់ទី ១១",
            text = "សារសំឡេង (7 វិនាទី)",
            voiceAudioBase64 = "BBBB",
            voiceDurationSec = 7,
        )
        var selectedConnId by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                StudyChatScreen(
                    currentUserId = "u1",
                    suggestedName = "សុខា",
                    accountState = UiState.Success(account),
                    roomsState = UiState.Success(listOf(room)),
                    messagesState = UiState.Success(listOf(voiceRoomMsg)),
                    selectedRoomId = "math_qa",
                    isAccountLocked = false,
                    isSavingAccount = false,
                    isSendingMessage = false,
                    pendingAttachment = null,
                    localLessons = emptyList(),
                    localSubjects = emptyList(),
                    onSaveAccount = { _, _, _, _ -> },
                    onUnlockAccount = { _, _ -> true },
                    onLockAccount = {},
                    onSignOut = {},
                    onSelectRoom = {},
                    onCreateRoom = { _, _, _ -> },
                    onAttachLesson = { _, _ -> },
                    onClearAttachment = {},
                    onSendMessage = { _, _, _ -> },
                    onDeleteMessage = {},
                    friendConnectionsState = UiState.Success(listOf(friendConn)),
                    directMessagesState = UiState.Success(listOf(voiceDirectMsg)),
                    selectedFriendConnectionId = selectedConnId,
                    onSelectFriendConnection = { selectedConnId = it },
                )
            }
        }

        // Group chat displays voice message card and mic button
        composeTestRule.onNodeWithTag("voice_message_card_vm1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("record_voice_chat_button").assertIsDisplayed()

        // Switch to Friends tab: Create Group shortcut is visible, then enter DM to verify voice card + mic
        composeTestRule.onNodeWithTag("chat_mode_friends_tab").performClick()
        composeTestRule.onNodeWithTag("friends_overview_list")
            .performScrollToNode(hasTestTag("friends_create_group_button"))
        composeTestRule.onNodeWithTag("friends_create_group_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("friends_overview_list")
            .performScrollToNode(hasTestTag("friend_item_peer_c_u1"))
        composeTestRule.onNodeWithTag("friend_item_peer_c_u1").performClick()

        composeTestRule.onNodeWithTag("voice_message_card_dm_v1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("record_voice_direct_button").assertIsDisplayed()
    }

    @Test
    fun `quick categorize dialog applies Math, Science, Personal and custom tags`() {
        val subjects = listOf(
            SubjectEntity(id = 1L, name = "គណិតវិទ្យា", colorHex = 0xFFD97706, iconKey = "math"),
            SubjectEntity(id = 2L, name = "វិទ្យាសាស្ត្រ", colorHex = 0xFF2E6F5E, iconKey = "science"),
        )
        val lesson = LessonEntity(
            id = 10L,
            subjectId = 1L,
            title = "សៀវភៅរូបវិទ្យា",
            type = LessonType.PDF,
        )
        var savedSubjectId = 0L
        var savedLabels = emptyList<String>()

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                QuickCategorizeDialog(
                    lesson = lesson,
                    subjects = subjects,
                    availableLabels = emptyList(),
                    onRequestAddCategory = {},
                    onDismiss = {},
                    onConfirm = { subId, labels ->
                        savedSubjectId = subId
                        savedLabels = labels
                    },
                )
            }
        }

        composeTestRule.onNodeWithTag("category_picker_chip_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("label_picker_chip_Science").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("label_picker_chip_Personal").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("custom_label_input").performScrollTo().performTextInput("ExamPrep")
        composeTestRule.onNodeWithTag("add_custom_label_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("confirm_categorize_button").performClick()

        assertEquals(2L, savedSubjectId)
        assertEquals(listOf("Science", "Personal", "ExamPrep"), savedLabels)
    }

    @Test
    fun `library screen and subjects hub display tag counts and trigger tag actions`() {
        var filteredLabel: String? = null
        var openedHubLabel = ""
        var renamedHubLabel = ""

        val subjects = listOf(
            SubjectEntity(id = 1L, name = "គណិតវិទ្យា", colorHex = 0xFFD97706, iconKey = "math"),
        )
        val lessons = listOf(
            LessonEntity(
                id = 1L,
                subjectId = 1L,
                title = "Algebra",
                type = LessonType.NOTE,
                labels = "Math, Personal",
            ),
        )
        var showHub by mutableStateOf(false)

        composeTestRule.setContent {
            LessonVaultTheme(darkTheme = false) {
                if (!showHub) {
                    LibraryScreen(
                        lessons = lessons,
                        allLessonsCount = 1,
                        subjects = subjects,
                        categoryCounts = mapOf(1L to 1),
                        availableLabels = listOf("Math", "Personal"),
                        labelCounts = mapOf("Math" to 1, "Personal" to 1),
                        stats = VaultStats(totalLessons = 1, totalSubjects = 1),
                        searchQuery = "",
                        selectedType = null,
                        selectedSubjectId = null,
                        selectedLabel = filteredLabel,
                        sortOrder = LessonSortOrder.DATE_CREATED,
                        onSearchChange = {},
                        onSelectType = {},
                        onSelectSubject = {},
                        onSelectLabel = { filteredLabel = it },
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
                } else {
                    SubjectsScreen(
                        subjects = subjects,
                        allLessons = lessons,
                        availableLabels = listOf("Math", "Personal"),
                        labelCounts = mapOf("Math" to 1, "Personal" to 1),
                        onOpenSubject = {},
                        onAddSubject = {},
                        onEditSubject = {},
                        onDeleteSubject = {},
                        onOpenLabel = { openedHubLabel = it },
                        onRenameLabel = { renamedHubLabel = it },
                        onDeleteLabel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("saved_lessons_list")
            .performScrollToNode(hasTestTag("label_filter_row"))
        composeTestRule.onNodeWithTag("filter_label_Math")
            .performScrollTo()
            .assertTextContains("Math (1)")
            .performClick()
        assertEquals("Math", filteredLabel)

        composeTestRule.onNodeWithTag("saved_lessons_list")
            .performScrollToNode(hasTestTag("lesson_card_1"))
        composeTestRule.onNodeWithTag("card_quick_tag_button_1").assertIsDisplayed()

        showHub = true
        composeTestRule.onNodeWithTag("subjects_hub_grid")
            .performScrollToNode(hasTestTag("hub_tag_chip_Personal"))
        composeTestRule.onNodeWithTag("hub_tag_chip_Personal")
            .assertTextContains("Personal (1)")
            .performClick()
        assertEquals("Personal", openedHubLabel)

        composeTestRule.onNodeWithTag("subjects_hub_grid")
            .performScrollToNode(hasTestTag("rename_tag_button_Math"))
        composeTestRule.onNodeWithTag("rename_tag_button_Math").performClick()
        assertEquals("Math", renamedHubLabel)
    }
}
