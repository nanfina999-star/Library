package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.LessonAttachmentDraft
import com.example.data.cloud.PasscodeHasher
import com.example.data.cloud.VoiceAttachmentDraft
import com.example.data.cloud.buildChatMessageCreateMap
import com.example.data.cloud.buildDirectLessonMessageCreateMap
import com.example.data.cloud.buildFriendConnectionCreateMap
import com.example.data.cloud.buildStudentProfileCreateMap
import com.example.data.cloud.buildStudyRoomCreateMap
import com.example.data.cloud.buildUserAccountCreateMap
import com.example.data.cloud.canonicalPairId
import com.example.data.cloud.sortedParticipantList
import com.example.data.cloud.studentCodeForUid
import com.example.data.local.LessonType
import com.example.data.local.LessonVaultDatabase
import com.example.data.local.labelList
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.InMemoryThemePreferencesRepository
import com.example.data.repository.LessonRepository
import com.example.data.storage.VaultFileStorage
import com.example.ui.LessonSortOrder
import com.example.ui.LessonVaultViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: LessonVaultDatabase
    private lateinit var storage: VaultFileStorage
    private lateinit var repository: LessonRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LessonVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = VaultFileStorage(context)
        repository = LessonRepository(db.subjectDao(), db.lessonDao(), storage)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches metadata`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("បណ្ដុំឯកសារមេរៀន", appName)
    }

    @Test
    fun `repository seeds default subjects including history and saves labeled note lesson`() = runBlocking {
        repository.ensureDefaultSubjects()
        val subjects = repository.subjects.first()
        assertEquals(6, subjects.size)
        assertTrue(subjects.any { it.name == "ប្រវត្តិសាស្ត្រ" })

        val savedId = repository.saveNoteLesson(
            lessonId = null,
            title = "មេរៀនគណិតវិទ្យាទី១",
            description = "អាំងតេក្រាល",
            subjectId = subjects.first { it.name == "គណិតវិទ្យា" }.id,
            noteContent = "រូបមន្តមូលដ្ឋាន...",
            labels = listOf("លំហាត់", "ត្រៀមប្រឡង"),
        )

        val lessons = repository.allLessons.first()
        assertEquals(1, lessons.size)
        assertEquals(savedId, lessons.first().id)
        assertEquals(LessonType.NOTE, lessons.first().type)
        assertEquals(listOf("លំហាត់", "ត្រៀមប្រឡង"), lessons.first().labelList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel filters lessons by category and label`() = runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            repository.ensureDefaultSubjects()
            val subjects = repository.subjects.first()
            val mathId = subjects.first { it.name == "គណិតវិទ្យា" }.id
            val historyId = subjects.first { it.name == "ប្រវត្តិសាស្ត្រ" }.id

            repository.saveNoteLesson(
                lessonId = null,
                title = "អាំងតេក្រាល",
                description = "",
                subjectId = mathId,
                noteContent = "Math formulas",
                labels = listOf("លំហាត់"),
            )
            repository.saveNoteLesson(
                lessonId = null,
                title = "សម័យអង្គរ",
                description = "",
                subjectId = historyId,
                noteContent = "History notes",
                labels = listOf("សង្ខេប"),
            )

            val viewModel = LessonVaultViewModel(repository)
            val collector = launch {
                viewModel.filteredLessons.collect {}
            }

            // Wait until both lessons are loaded
            val both = viewModel.filteredLessons.first { it.size == 2 }
            assertEquals(2, both.size)

            // Filter by History category
            viewModel.setSubjectFilter(historyId)
            val historyOnly = viewModel.filteredLessons.first { it.size == 1 }
            assertEquals("សម័យអង្គរ", historyOnly.first().title)

            // Clear category and filter by label
            viewModel.clearFilters()
            viewModel.setLabelFilter("លំហាត់")
            val labeledOnly = viewModel.filteredLessons.first { it.size == 1 }
            assertEquals("អាំងតេក្រាល", labeledOnly.first().title)

            collector.cancel()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @Test
    fun `importing local file copies bytes into app internal storage and deletes cleanly`() = runBlocking {
        repository.ensureDefaultSubjects()
        val subjectId = repository.subjects.first().first().id

        val tempSource = File(context.cacheDir, "sample_lesson.txt").apply {
            writeText("មេរៀនទី ២៖ រូបវិទ្យា")
        }
        val staged = repository.stageImportFromUri(Uri.fromFile(tempSource), forceImage = false)
        assertEquals(LessonType.NOTE, staged.detectedType)
        assertTrue(staged.fileSizeBytes > 0L)

        val id = repository.commitImportedLesson(
            staged = staged,
            title = "រូបវិទ្យាជំពូក២",
            description = "សង្ខេបមេរៀន",
            subjectId = subjectId,
            studyNote = "",
            labels = listOf("សង្ខេប"),
        )

        val savedLesson = repository.observeLesson(id).first()
        assertNotNull(savedLesson)
        val internalFile = repository.resolveLessonFile(savedLesson?.internalFilePath)
        assertNotNull(internalFile)
        assertTrue(internalFile!!.exists())

        repository.deleteLesson(savedLesson!!)
        assertFalse(internalFile.exists())
        assertTrue(repository.allLessons.first().isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel toggles between light and dark theme mode`() = runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            val themePrefs = InMemoryThemePreferencesRepository(AppThemeMode.LIGHT)
            val viewModel = LessonVaultViewModel(repository, themePrefs)
            val collector = launch { viewModel.themeMode.collect {} }

            assertEquals(AppThemeMode.LIGHT, viewModel.themeMode.first { it == AppThemeMode.LIGHT })

            viewModel.toggleTheme(currentlyDark = false)
            assertEquals(AppThemeMode.DARK, viewModel.themeMode.first { it == AppThemeMode.DARK })

            viewModel.toggleTheme(currentlyDark = true)
            assertEquals(AppThemeMode.LIGHT, viewModel.themeMode.first { it == AppThemeMode.LIGHT })

            collector.cancel()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel sorts documents by date created, name, and category`() = runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            repository.ensureDefaultSubjects()
            val subjects = repository.subjects.first()
            // Subjects include គណិតវិទ្យា (Math), ប្រវត្តិសាស្ត្រ (History), រូបវិទ្យា (Physics), etc.
            val mathId = subjects.first { it.name == "គណិតវិទ្យា" }.id
            val historyId = subjects.first { it.name == "ប្រវត្តិសាស្ត្រ" }.id

            val firstId = repository.saveNoteLesson(
                lessonId = null,
                title = "Beta Lesson",
                description = "",
                subjectId = historyId,
                noteContent = "History",
            )
            // Ensure distinct createdAt timestamps
            val secondId = repository.saveNoteLesson(
                lessonId = null,
                title = "Alpha Lesson",
                description = "",
                subjectId = mathId,
                noteContent = "Math",
            )
            // Make sure createdAt order is strictly controlled in DB
            val dao = db.lessonDao()
            val firstEntity = dao.getLessonById(firstId)!!.copy(createdAt = 1_000L)
            val secondEntity = dao.getLessonById(secondId)!!.copy(createdAt = 2_000L)
            dao.updateLesson(firstEntity)
            dao.updateLesson(secondEntity)

            val viewModel = LessonVaultViewModel(repository)
            val collector = launch { viewModel.filteredLessons.collect {} }

            // 1. Default DATE_CREATED (newest createdAt first -> Alpha Lesson created at 2_000L)
            val byDateCreated = viewModel.filteredLessons.first { it.size == 2 }
            assertEquals(listOf("Alpha Lesson", "Beta Lesson"), byDateCreated.map { it.title })

            // 2. NAME (alphabetical -> Alpha Lesson, Beta Lesson)
            viewModel.setSortOrder(LessonSortOrder.NAME)
            val thirdId = repository.saveNoteLesson(
                lessonId = null,
                title = "Gamma Lesson",
                description = "",
                subjectId = mathId,
                noteContent = "More math",
            )
            val thirdEntity = dao.getLessonById(thirdId)!!.copy(createdAt = 3_000L)
            dao.updateLesson(thirdEntity)

            val byName = viewModel.filteredLessons.first {
                it.size == 3 && it.map { l -> l.title } == listOf("Alpha Lesson", "Beta Lesson", "Gamma Lesson")
            }
            assertEquals(listOf("Alpha Lesson", "Beta Lesson", "Gamma Lesson"), byName.map { it.title })

            // 3. CATEGORY (alphabetical by subject name: គណិតវិទ្យា comes before ប្រវត្តិសាស្ត្រ in Khmer Unicode)
            viewModel.setSortOrder(LessonSortOrder.CATEGORY)
            val byCategory = viewModel.filteredLessons.first {
                it.size == 3 && it.last().title == "Beta Lesson"
            }
            assertEquals(listOf("Alpha Lesson", "Gamma Lesson", "Beta Lesson"), byCategory.map { it.title })

            // 4. DATE_CREATED newest first -> Gamma, Alpha, Beta
            viewModel.setSortOrder(LessonSortOrder.DATE_CREATED)
            val backToDate = viewModel.filteredLessons.first {
                it.size == 3 && it.first().title == "Gamma Lesson"
            }
            assertEquals(listOf("Gamma Lesson", "Alpha Lesson", "Beta Lesson"), backToDate.map { it.title })

            collector.cancel()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel toggles UI tap sound effects preference`() = runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            val prefs = InMemoryThemePreferencesRepository(initialSoundEnabled = true)
            val viewModel = LessonVaultViewModel(repository, prefs)
            val collector = launch { viewModel.soundEffectsEnabled.collect {} }

            assertTrue(viewModel.soundEffectsEnabled.first { it })

            viewModel.toggleSoundEffects(currentlyEnabled = true)
            assertFalse(viewModel.soundEffectsEnabled.first { !it })

            viewModel.toggleSoundEffects(currentlyEnabled = false)
            assertTrue(viewModel.soundEffectsEnabled.first { it })

            collector.cancel()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @Test
    fun `passcode hasher generates salted PBKDF2 hash and verifies accurately`() {
        val first = PasscodeHasher.hashNewPasscode("123456")
        val second = PasscodeHasher.hashNewPasscode("123456")

        assertTrue(first.saltHex.length == 32)
        assertTrue(first.hashHex.length == 64)
        assertFalse(first.saltHex == second.saltHex)
        assertTrue(PasscodeHasher.verifyPasscode("123456", first.saltHex, first.hashHex))
        assertFalse(PasscodeHasher.verifyPasscode("654321", first.saltHex, first.hashHex))
        assertFalse(PasscodeHasher.verifyPasscode("", first.saltHex, first.hashHex))
    }

    @Test
    fun `cloud payload builders omit nulls and preserve security rule fields`() {
        val accountMap = buildUserAccountCreateMap(
            userId = "uid_123",
            displayName = "សុខា",
            gradeLevel = "ថ្នាក់ទី ១២",
            passcodeHash = "abcd",
            passcodeSalt = "ef01",
        )
        assertEquals(
            setOf("userId", "displayName", "gradeLevel", "passcodeHash", "passcodeSalt", "createdAt", "updatedAt"),
            accountMap.keys,
        )

        val roomMap = buildStudyRoomCreateMap(
            creatorId = "uid_123",
            name = "គណិតវិទ្យា",
            description = "សួរលំហាត់",
            subjectTag = "គណិតវិទ្យា",
        )
        assertEquals("shared", roomMap["visibility"])
        assertEquals(
            setOf("creatorId", "name", "description", "subjectTag", "visibility", "createdAt", "updatedAt"),
            roomMap.keys,
        )

        val plainMsg = buildChatMessageCreateMap(
            roomId = "general_study",
            senderId = "uid_123",
            senderName = "សុខា",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "សួស្តីអ្នកទាំងអស់គ្នា!",
            attachment = null,
        )
        assertEquals(
            setOf("roomId", "senderId", "senderName", "senderGrade", "text", "createdAt"),
            plainMsg.keys,
        )

        val attachedMsg = buildChatMessageCreateMap(
            roomId = "general_study",
            senderId = "uid_123",
            senderName = "សុខា",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "សូមជួយពន្យល់មេរៀននេះផង",
            attachment = LessonAttachmentDraft(
                title = "អាំងតេក្រាល",
                typeLabel = "កំណត់ត្រា",
                subjectName = "គណិតវិទ្យា",
                noteExcerpt = "រូបមន្តមូលដ្ឋាន",
            ),
        )
        assertEquals("អាំងតេក្រាល", attachedMsg["attachedLessonTitle"])
        assertEquals("គណិតវិទ្យា", attachedMsg["attachedLessonSubject"])
    }

    @Test
    fun `friend connection helpers build canonical pair IDs and null-safe direct chat payloads`() {
        assertEquals(listOf("alice", "bob"), sortedParticipantList("bob", "alice"))
        assertEquals("alice_bob", canonicalPairId("bob", "alice"))
        assertEquals("alice_bob", canonicalPairId("alice", "bob"))
        assertEquals("ABC123", studentCodeForUid("a-bc123456"))

        val profileMap = buildStudentProfileCreateMap(
            userId = "alice",
            displayName = "សុខា",
            gradeLevel = "ថ្នាក់ទី ១២",
        )
        assertEquals("students", profileMap["visibility"])
        assertEquals(
            setOf("userId", "displayName", "gradeLevel", "studentCode", "visibility", "createdAt", "updatedAt"),
            profileMap.keys,
        )

        val friendReqMap = buildFriendConnectionCreateMap(
            requesterId = "bob",
            requesterName = "បុប្ផា",
            requesterGrade = "ថ្នាក់ទី ១១",
            recipientId = "alice",
            recipientName = "សុខា",
            recipientGrade = "ថ្នាក់ទី ១២",
        )
        assertEquals(listOf("alice", "bob"), friendReqMap["participantIds"])
        assertEquals("pending", friendReqMap["status"])
        assertEquals(
            setOf(
                "participantIds",
                "requesterId",
                "requesterName",
                "requesterGrade",
                "recipientId",
                "recipientName",
                "recipientGrade",
                "status",
                "createdAt",
                "updatedAt",
            ),
            friendReqMap.keys,
        )

        val plainDm = buildDirectLessonMessageCreateMap(
            connectionId = "alice_bob",
            participantIds = listOf("alice", "bob"),
            senderId = "alice",
            senderName = "សុខា",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "ជួយមើលលំហាត់ទី៣ផង",
            attachment = null,
        )
        assertEquals(
            setOf("connectionId", "participantIds", "senderId", "senderName", "senderGrade", "text", "createdAt"),
            plainDm.keys,
        )

        val attachedDm = buildDirectLessonMessageCreateMap(
            connectionId = "alice_bob",
            participantIds = listOf("alice", "bob"),
            senderId = "alice",
            senderName = "សុខា",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "ផ្ញើមេរៀនរូបវិទ្យា",
            attachment = LessonAttachmentDraft(
                title = "ច្បាប់ញូតុន",
                typeLabel = "PDF",
                subjectName = "រូបវិទ្យា",
                noteExcerpt = "F = m * a",
            ),
        )
        assertEquals("ច្បាប់ញូតុន", attachedDm["attachedLessonTitle"])
        assertEquals("រូបវិទ្យា", attachedDm["attachedLessonSubject"])
        assertEquals("F = m * a", attachedDm["attachedLessonNote"])

        val voiceDm = buildDirectLessonMessageCreateMap(
            connectionId = "alice_bob",
            participantIds = listOf("alice", "bob"),
            senderId = "alice",
            senderName = "សុខា",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "សារសំឡេង (9 វិនាទី)",
            attachment = null,
            voice = VoiceAttachmentDraft(audioBase64 = "AAAA_M4A", durationSec = 9),
        )
        assertEquals("AAAA_M4A", voiceDm["voiceAudioBase64"])
        assertEquals(9, voiceDm["voiceDurationSec"])

        val voiceRoom = buildChatMessageCreateMap(
            roomId = "math_qa",
            senderId = "alice",
            senderName = "សុខា",
            senderGrade = "ថ្នាក់ទី ១២",
            text = "សារសំឡេង (15 វិនាទី)",
            attachment = null,
            voice = VoiceAttachmentDraft(audioBase64 = "BBBB_M4A", durationSec = 15),
        )
        assertEquals("BBBB_M4A", voiceRoom["voiceAudioBase64"])
        assertEquals(15, voiceRoom["voiceDurationSec"])
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel search query filters lessons by title`() = runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            repository.ensureDefaultSubjects()
            val subjectId = repository.subjects.first().first().id
            repository.saveNoteLesson(null, "គណិតវិទ្យា៖ អាំងតេក្រាល", "", subjectId, "រូបមន្ត")
            repository.saveNoteLesson(null, "ប្រវត្តិសាស្ត្រអង្គរ", "", subjectId, "សង្ខេប")

            val viewModel = LessonVaultViewModel(repository)
            val collector = launch { viewModel.filteredLessons.collect {} }

            viewModel.filteredLessons.first { it.size == 2 }
            viewModel.setSearchQuery("អាំងតេក្រាល")
            val matched = viewModel.filteredLessons.first { it.size == 1 }
            assertEquals("គណិតវិទ្យា៖ អាំងតេក្រាល", matched.first().title)

            collector.cancel()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `categorization system supports Math, Science, Personal tags, counts, rename, and delete`() = runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            repository.ensureDefaultSubjects()
            val subjects = repository.subjects.first()
            val mathSubjectId = subjects.first { it.name == "គណិតវិទ្យា" }.id
            val scienceSubjectId = subjects.first { it.name == "វិទ្យាសាស្ត្រ" }.id

            val id1 = repository.saveNoteLesson(
                lessonId = null,
                title = "Calculus Notes",
                description = "",
                subjectId = mathSubjectId,
                noteContent = "Derivatives",
                labels = listOf("Math", "math", "Personal"),
            )
            val id2 = repository.saveNoteLesson(
                lessonId = null,
                title = "Physics Lab",
                description = "",
                subjectId = scienceSubjectId,
                noteContent = "Optics",
                labels = listOf("Science", "Personal"),
            )

            val viewModel = LessonVaultViewModel(repository)
            val labelsCollector = launch { viewModel.availableLabels.collect {} }
            val countsCollector = launch { viewModel.labelCounts.collect {} }
            val filteredCollector = launch { viewModel.filteredLessons.collect {} }

            val labels = viewModel.availableLabels.first { it.size == 3 }
            assertEquals(listOf("Math", "Personal", "Science"), labels)

            val counts = viewModel.labelCounts.first { it["Personal"] == 2 }
            assertEquals(1, counts["Math"])
            assertEquals(1, counts["Science"])
            assertEquals(2, counts["Personal"])

            // Filter by Personal tag
            viewModel.selectTagForLibrary("Personal")
            assertEquals(2, viewModel.filteredLessons.first { it.size == 2 }.size)

            // Rename Science -> Biology across lessons
            viewModel.renameTag("Science", "Biology")
            val updatedLabels = viewModel.availableLabels.first { it.contains("Biology") }
            assertFalse(updatedLabels.contains("Science"))

            // Delete Personal tag from all lessons
            viewModel.deleteTag("Personal")
            val afterDeleteLabels = viewModel.availableLabels.first { !it.contains("Personal") }
            assertEquals(listOf("Biology", "Math"), afterDeleteLabels)

            // Update category and labels for a single lesson
            viewModel.updateLessonCategoryAndLabels(id1, scienceSubjectId, listOf("Math", "Personal"))
            val updatedLesson = repository.observeLesson(id1).first {
                it?.subjectId == scienceSubjectId && it.labelList().contains("Personal")
            }
            assertNotNull(updatedLesson)
            assertEquals(listOf("Math", "Personal"), updatedLesson!!.labelList())
            assertTrue(id2 > 0L)

            labelsCollector.cancel()
            countsCollector.cancel()
            filteredCollector.cancel()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }
}
