package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.cloud.StudyCloudRepository
import com.example.data.cloud.attemptAutoSignIn
import com.example.data.cloud.authStateFlow
import com.example.data.cloud.signOutWithCleanup
import com.example.data.local.LessonEntity
import com.example.data.local.LessonVaultDatabase
import com.example.data.local.SubjectEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.DataStoreThemePreferencesRepository
import com.example.data.repository.LessonRepository
import com.example.data.storage.VaultFileStorage
import com.example.ui.LessonVaultViewModel
import com.example.ui.LessonVaultViewModelFactory
import com.example.ui.StudyChatViewModel
import com.example.ui.audio.AndroidUiSoundPlayer
import com.example.ui.audio.LocalUiSoundEnabled
import com.example.ui.audio.LocalUiSoundPlayer
import com.example.ui.audio.SoundToggleIconButton
import com.example.ui.audio.detectGlobalUiTapSound
import com.example.ui.components.AddLessonActionSheet
import com.example.ui.components.AuthSignInGateScreen
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EditLessonMetadataDialog
import com.example.ui.components.ImportLessonDialog
import com.example.ui.components.QuickCategorizeDialog
import com.example.ui.components.RenameTagDialog
import com.example.ui.components.SubjectEditorDialog
import com.example.ui.components.ThemeToggleButton
import com.example.ui.components.TopBarSortMenuButton
import com.example.ui.screens.LessonViewerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PinnedScreen
import com.example.ui.screens.StudyChatScreen
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.theme.LessonVaultTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object Routes {
    const val LIBRARY = "library"
    const val SUBJECTS = "subjects"
    const val PINNED = "pinned"
    const val CHAT = "chat"
    const val SUBJECT_DETAIL = "subject_detail/{subjectId}"
    const val VIEWER = "viewer/{lessonId}"
    const val NOTE_EDITOR = "note_editor?lessonId={lessonId}&subjectId={subjectId}"

    fun subjectDetail(subjectId: Long) = "subject_detail/$subjectId"
    fun viewer(lessonId: Long) = "viewer/$lessonId"
    fun noteEditor(lessonId: Long = -1L, subjectId: Long = -1L) =
        "note_editor?lessonId=$lessonId&subjectId=$subjectId"
}

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val TopLevelTabs = listOf(
    TopLevelTab(
        route = Routes.LIBRARY,
        labelRes = R.string.nav_library,
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        testTag = "nav_tab_library",
    ),
    TopLevelTab(
        route = Routes.SUBJECTS,
        labelRes = R.string.nav_subjects,
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder,
        testTag = "nav_tab_subjects",
    ),
    TopLevelTab(
        route = Routes.PINNED,
        labelRes = R.string.nav_pinned,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "nav_tab_pinned",
    ),
    TopLevelTab(
        route = Routes.CHAT,
        labelRes = R.string.nav_chat,
        selectedIcon = Icons.Filled.Forum,
        unselectedIcon = Icons.Outlined.Forum,
        testTag = "nav_tab_chat",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = LessonVaultDatabase.getInstance(applicationContext)
        val storage = VaultFileStorage(applicationContext)
        val repository = LessonRepository(db.subjectDao(), db.lessonDao(), storage)
        val themePrefs = DataStoreThemePreferencesRepository(applicationContext)
        val factory = LessonVaultViewModelFactory(repository, themePrefs)

        setContent {
            val viewModel: LessonVaultViewModel = viewModel(factory = factory)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val soundEnabled by viewModel.soundEffectsEnabled.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val soundPlayer = remember { AndroidUiSoundPlayer() }

            DisposableEffect(soundPlayer) {
                onDispose { soundPlayer.release() }
            }

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT,
                    ) { useDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF),
                        AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B),
                    ) { useDarkTheme },
                )
                onDispose {}
            }

            CompositionLocalProvider(
                LocalUiSoundPlayer provides soundPlayer,
                LocalUiSoundEnabled provides soundEnabled,
            ) {
                LessonVaultTheme(darkTheme = useDarkTheme) {
                    LessonVaultApp(
                        viewModel = viewModel,
                        isDarkTheme = useDarkTheme,
                        soundEnabled = soundEnabled,
                        onToggleTheme = { viewModel.toggleTheme(useDarkTheme) },
                        onToggleSound = { viewModel.toggleSoundEffects(soundEnabled) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonVaultApp(
    viewModel: LessonVaultViewModel,
    isDarkTheme: Boolean,
    soundEnabled: Boolean,
    onToggleTheme: () -> Unit,
    onToggleSound: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelRoute = currentRoute in setOf(Routes.LIBRARY, Routes.SUBJECTS, Routes.PINNED, Routes.CHAT)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser,
    )
    var pendingChatLesson by remember { mutableStateOf<Pair<LessonEntity, SubjectEntity?>?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = coroutineScope,
        )
    }

    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val allLessons by viewModel.allLessons.collectAsStateWithLifecycle()
    val filteredLessons by viewModel.filteredLessons.collectAsStateWithLifecycle()
    val pinnedLessons by viewModel.pinnedLessons.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val availableLabels by viewModel.availableLabels.collectAsStateWithLifecycle()
    val labelCounts by viewModel.labelCounts.collectAsStateWithLifecycle()
    val vaultStats by viewModel.vaultStats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedSubjectFilter by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()
    val selectedLabelFilter by viewModel.selectedLabelFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val stagedImport by viewModel.stagedImport.collectAsStateWithLifecycle()
    val preselectedSubjectId by viewModel.preselectedSubjectIdForAdd.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showAddSheet by remember { mutableStateOf(false) }
    var categorizingLesson by remember { mutableStateOf<LessonEntity?>(null) }
    var editingMetadataLesson by remember { mutableStateOf<LessonEntity?>(null) }
    var deletingLesson by remember { mutableStateOf<LessonEntity?>(null) }
    var showSubjectEditor by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var deletingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var renamingTag by remember { mutableStateOf<String?>(null) }
    var deletingTag by remember { mutableStateOf<String?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onUriPicked(uri, isImage = false)
            }
        },
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onUriPicked(uri, isImage = true)
            }
        },
    )

    val launchDocumentPicker = {
        documentPickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "text/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "*/*",
            ),
        )
    }

    val launchImagePicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val openCreateNote = { subjectId: Long? ->
        navController.navigate(Routes.noteEditor(lessonId = -1L, subjectId = subjectId ?: -1L))
    }

    val openAddCategoryDialog = {
        editingSubject = null
        showSubjectEditor = true
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .detectGlobalUiTapSound(),
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        actions = {
                            if (currentRoute == Routes.LIBRARY) {
                                TopBarSortMenuButton(
                                    currentOrder = sortOrder,
                                    onSelectOrder = viewModel::setSortOrder,
                                )
                            }
                            SoundToggleIconButton(
                                soundEnabled = soundEnabled,
                                onToggle = onToggleSound,
                            )
                            if (currentRoute != Routes.CHAT) {
                                IconButton(
                                    onClick = {
                                        navController.navigate(Routes.CHAT) {
                                            popUpTo(Routes.LIBRARY) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("top_bar_chat_account_button"),
                                ) {
                                    Icon(
                                        imageVector = if (currentUser != null) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                                        contentDescription = stringResource(R.string.nav_chat),
                                        tint = if (currentUser != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            ThemeToggleButton(
                                isDarkTheme = isDarkTheme,
                                onToggle = onToggleTheme,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.LIBRARY) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isTopLevelRoute && currentRoute != Routes.CHAT) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.setPreselectedSubjectForAdd(selectedSubjectFilter)
                            showAddSheet = true
                        },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.action_add_lesson)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_lesson_fab"),
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isTopLevelRoute && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        header = {
                            if (currentRoute != Routes.CHAT) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FloatingActionButton(
                                    onClick = {
                                        viewModel.setPreselectedSubjectForAdd(selectedSubjectFilter)
                                        showAddSheet = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_lesson))
                                }
                            }
                        },
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.LIBRARY) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.LIBRARY,
                    ) {
                        composable(Routes.LIBRARY) {
                            LibraryScreen(
                                lessons = filteredLessons,
                                allLessonsCount = allLessons.size,
                                subjects = subjects,
                                categoryCounts = categoryCounts,
                                availableLabels = availableLabels,
                                labelCounts = labelCounts,
                                stats = vaultStats,
                                searchQuery = searchQuery,
                                selectedType = selectedType,
                                selectedSubjectId = selectedSubjectFilter,
                                selectedLabel = selectedLabelFilter,
                                sortOrder = sortOrder,
                                onSearchChange = viewModel::setSearchQuery,
                                onSelectType = viewModel::setTypeFilter,
                                onSelectSubject = viewModel::setSubjectFilter,
                                onSelectLabel = viewModel::setLabelFilter,
                                onAddCategory = openAddCategoryDialog,
                                onSelectSortOrder = viewModel::setSortOrder,
                                onClearFilters = viewModel::clearFilters,
                                onOpenLesson = { lesson -> navController.navigate(Routes.viewer(lesson.id)) },
                                onTogglePin = viewModel::togglePinned,
                                onCategorizeLesson = { categorizingLesson = it },
                                onEditLesson = { editingMetadataLesson = it },
                                onDeleteLesson = { deletingLesson = it },
                                onQuickImportDocument = {
                                    viewModel.setPreselectedSubjectForAdd(selectedSubjectFilter)
                                    launchDocumentPicker()
                                },
                                onQuickImportImage = {
                                    viewModel.setPreselectedSubjectForAdd(selectedSubjectFilter)
                                    launchImagePicker()
                                },
                                onQuickCreateNote = { openCreateNote(selectedSubjectFilter) },
                            )
                        }

                        composable(Routes.SUBJECTS) {
                            SubjectsScreen(
                                subjects = subjects,
                                allLessons = allLessons,
                                availableLabels = availableLabels,
                                labelCounts = labelCounts,
                                onOpenSubject = { subject ->
                                    navController.navigate(Routes.subjectDetail(subject.id))
                                },
                                onAddSubject = openAddCategoryDialog,
                                onEditSubject = { subject ->
                                    editingSubject = subject
                                    showSubjectEditor = true
                                },
                                onDeleteSubject = { deletingSubject = it },
                                onOpenLabel = { label ->
                                    viewModel.selectTagForLibrary(label)
                                    navController.navigate(Routes.LIBRARY) {
                                        popUpTo(Routes.LIBRARY) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onRenameLabel = { renamingTag = it },
                                onDeleteLabel = { deletingTag = it },
                            )
                        }

                        composable(Routes.PINNED) {
                            PinnedScreen(
                                pinnedLessons = pinnedLessons,
                                recentLessons = allLessons,
                                subjects = subjects,
                                onOpenLesson = { lesson -> navController.navigate(Routes.viewer(lesson.id)) },
                                onTogglePin = viewModel::togglePinned,
                                onCategorizeLesson = { categorizingLesson = it },
                                onEditLesson = { editingMetadataLesson = it },
                                onDeleteLesson = { deletingLesson = it },
                            )
                        }

                        composable(Routes.CHAT) {
                            val activeUser = currentUser
                            if (activeUser == null) {
                                AuthSignInGateScreen(onAuthSuccess = {})
                            } else {
                                val uid = activeUser.uid
                                val suggestedName = activeUser.displayName.orEmpty()
                                key(uid) {
                                    val chatViewModel: StudyChatViewModel = viewModel(
                                        key = uid,
                                        factory = viewModelFactory {
                                            initializer {
                                                val dbId = context.applicationContext.getString(R.string.firestore_database_id)
                                                val repo = StudyCloudRepository(FirebaseFirestore.getInstance(dbId))
                                                StudyChatViewModel(repo, uid)
                                            }
                                        },
                                    )

                                    LaunchedEffect(chatViewModel) {
                                        chatViewModel.events.collect { message ->
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }

                                    LaunchedEffect(pendingChatLesson, chatViewModel) {
                                        pendingChatLesson?.let { (lessonToAttach, subj) ->
                                            chatViewModel.attachLesson(lessonToAttach, subj)
                                            pendingChatLesson = null
                                        }
                                    }

                                    val accountState by chatViewModel.accountState.collectAsStateWithLifecycle()
                                    val roomsState by chatViewModel.roomsState.collectAsStateWithLifecycle()
                                    val messagesState by chatViewModel.messagesState.collectAsStateWithLifecycle()
                                    val selectedRoomId by chatViewModel.selectedRoomId.collectAsStateWithLifecycle()
                                    val studentDirectoryState by chatViewModel.studentDirectoryState.collectAsStateWithLifecycle()
                                    val friendConnectionsState by chatViewModel.friendConnectionsState.collectAsStateWithLifecycle()
                                    val directMessagesState by chatViewModel.directMessagesState.collectAsStateWithLifecycle()
                                    val selectedFriendConnectionId by chatViewModel.selectedFriendConnectionId.collectAsStateWithLifecycle()
                                    val isAccountLocked by chatViewModel.isAccountLocked.collectAsStateWithLifecycle()
                                    val isSavingAccount by chatViewModel.isSavingAccount.collectAsStateWithLifecycle()
                                    val isSendingMessage by chatViewModel.isSendingMessage.collectAsStateWithLifecycle()
                                    val pendingAttachment by chatViewModel.pendingAttachment.collectAsStateWithLifecycle()

                                    StudyChatScreen(
                                        currentUserId = uid,
                                        suggestedName = suggestedName,
                                        accountState = accountState,
                                        roomsState = roomsState,
                                        messagesState = messagesState,
                                        selectedRoomId = selectedRoomId,
                                        isAccountLocked = isAccountLocked,
                                        isSavingAccount = isSavingAccount,
                                        isSendingMessage = isSendingMessage,
                                        pendingAttachment = pendingAttachment,
                                        localLessons = allLessons,
                                        localSubjects = subjects,
                                        onSaveAccount = { name, grade, password, existing ->
                                            chatViewModel.saveStudentAccount(name, grade, password, existing)
                                        },
                                        onUnlockAccount = chatViewModel::unlockWithPassword,
                                        onLockAccount = chatViewModel::lockAccount,
                                        onSignOut = {
                                            signOutWithCleanup(
                                                credentialManager = credentialManager,
                                                scope = coroutineScope,
                                            )
                                        },
                                        onSelectRoom = chatViewModel::selectRoom,
                                        onCreateRoom = { name, desc, tag ->
                                            chatViewModel.createStudyRoom(name, desc, tag)
                                        },
                                        onAttachLesson = chatViewModel::attachLesson,
                                        onClearAttachment = chatViewModel::clearPendingAttachment,
                                        onSendMessage = { account, text, onSent ->
                                            chatViewModel.sendMessage(account, text, onSent = onSent)
                                        },
                                        onDeleteMessage = chatViewModel::deleteMessage,
                                        studentDirectoryState = studentDirectoryState,
                                        friendConnectionsState = friendConnectionsState,
                                        directMessagesState = directMessagesState,
                                        selectedFriendConnectionId = selectedFriendConnectionId,
                                        onSelectFriendConnection = chatViewModel::selectFriendConnection,
                                        onSendFriendRequest = { myAccount, targetStudent, existingConn ->
                                            chatViewModel.sendFriendRequest(myAccount, targetStudent, existingConn)
                                        },
                                        onRespondFriendRequest = { conn, myAccount, accept ->
                                            chatViewModel.respondToFriendRequest(conn, myAccount, accept)
                                        },
                                        onRemoveFriendConnection = chatViewModel::removeFriendConnection,
                                        onSendDirectMessage = { conn, account, text, onSent ->
                                            chatViewModel.sendDirectMessage(conn, account, text, onSent = onSent)
                                        },
                                        onDeleteDirectMessage = chatViewModel::deleteDirectMessage,
                                        onSendVoiceMessage = { account, voice, caption, onSent ->
                                            chatViewModel.sendMessage(account, caption, voice = voice, onSent = onSent)
                                        },
                                        onSendDirectVoiceMessage = { conn, account, voice, caption, onSent ->
                                            chatViewModel.sendDirectMessage(conn, account, caption, voice = voice, onSent = onSent)
                                        },
                                        onShowInfoMessage = chatViewModel::emitInfo,
                                    )
                                }
                            }
                        }

                        composable(
                            route = Routes.SUBJECT_DETAIL,
                            arguments = listOf(navArgument("subjectId") { type = NavType.LongType }),
                        ) { entry ->
                            val subjectId = entry.arguments?.getLong("subjectId") ?: 0L
                            val subject = subjects.firstOrNull { it.id == subjectId }
                            val subjectLessons by viewModel.observeLessonsBySubject(subjectId)
                                .collectAsStateWithLifecycle(initialValue = emptyList())

                            SubjectDetailScreen(
                                subject = subject,
                                lessons = subjectLessons,
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = onToggleTheme,
                                onBack = { navController.popBackStack() },
                                onAddLessonToSubject = {
                                    viewModel.setPreselectedSubjectForAdd(subjectId)
                                    showAddSheet = true
                                },
                                onOpenLesson = { lesson -> navController.navigate(Routes.viewer(lesson.id)) },
                                onTogglePin = viewModel::togglePinned,
                                onCategorizeLesson = { categorizingLesson = it },
                                onEditLesson = { editingMetadataLesson = it },
                                onDeleteLesson = { deletingLesson = it },
                            )
                        }

                        composable(
                            route = Routes.VIEWER,
                            arguments = listOf(navArgument("lessonId") { type = NavType.LongType }),
                        ) { entry ->
                            val lessonId = entry.arguments?.getLong("lessonId") ?: 0L
                            val lesson by viewModel.observeLesson(lessonId)
                                .collectAsStateWithLifecycle(initialValue = null)
                            val subject = lesson?.let { l -> subjects.firstOrNull { it.id == l.subjectId } }

                            LessonViewerScreen(
                                lesson = lesson,
                                subject = subject,
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = onToggleTheme,
                                resolveFile = viewModel::resolveFile,
                                getShareUri = viewModel::getShareUri,
                                readTextFile = viewModel::readTextFile,
                                onBack = { navController.popBackStack() },
                                onTogglePin = viewModel::togglePinned,
                                onCategorize = { categorizingLesson = it },
                                onEditNoteLesson = { l ->
                                    navController.navigate(Routes.noteEditor(lessonId = l.id, subjectId = l.subjectId))
                                },
                                onEditMetadata = { editingMetadataLesson = it },
                                onUpdateLastPage = viewModel::updateLastPage,
                                onSaveStudyNotes = viewModel::updateStudyNotes,
                                onDeleteLesson = { l ->
                                    viewModel.deleteLesson(l) {
                                        navController.popBackStack()
                                    }
                                },
                                onAskInChat = { l ->
                                    pendingChatLesson = l to subject
                                    navController.navigate(Routes.CHAT) {
                                        popUpTo(Routes.LIBRARY) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }

                        composable(
                            route = Routes.NOTE_EDITOR,
                            arguments = listOf(
                                navArgument("lessonId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                },
                                navArgument("subjectId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                },
                            ),
                        ) { entry ->
                            val lessonId = entry.arguments?.getLong("lessonId") ?: -1L
                            val subjectIdArg = entry.arguments?.getLong("subjectId") ?: -1L
                            val existingLesson = if (lessonId > 0L) {
                                allLessons.firstOrNull { it.id == lessonId }
                            } else {
                                null
                            }

                            NoteEditorScreen(
                                existingLesson = existingLesson,
                                subjects = subjects,
                                availableLabels = availableLabels,
                                initialSubjectId = subjectIdArg.takeIf { it > 0L },
                                onRequestAddCategory = openAddCategoryDialog,
                                onBack = { navController.popBackStack() },
                                onSave = { title, desc, subjectId, content, labels ->
                                    viewModel.saveNoteLesson(
                                        lessonId = lessonId.takeIf { it > 0L },
                                        title = title,
                                        description = desc,
                                        subjectId = subjectId,
                                        noteContent = content,
                                        labels = labels,
                                    ) {
                                        navController.popBackStack()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddLessonActionSheet(
            onDismiss = { showAddSheet = false },
            onImportDocument = launchDocumentPicker,
            onImportImage = launchImagePicker,
            onCreateNote = { openCreateNote(preselectedSubjectId) },
        )
    }

    stagedImport?.let { staged ->
        ImportLessonDialog(
            staged = staged,
            subjects = subjects,
            availableLabels = availableLabels,
            initialSubjectId = preselectedSubjectId,
            onRequestAddCategory = openAddCategoryDialog,
            onDismiss = viewModel::cancelStagedImport,
            onConfirm = { title, desc, subjectId, studyNote, labels ->
                viewModel.commitStagedImport(
                    title = title,
                    description = desc,
                    subjectId = subjectId,
                    studyNote = studyNote,
                    labels = labels,
                )
            },
        )
    }

    categorizingLesson?.let { lesson ->
        QuickCategorizeDialog(
            lesson = lesson,
            subjects = subjects,
            availableLabels = availableLabels,
            onRequestAddCategory = openAddCategoryDialog,
            onDismiss = { categorizingLesson = null },
            onConfirm = { subjectId, labels ->
                viewModel.updateLessonCategoryAndLabels(lesson.id, subjectId, labels)
                categorizingLesson = null
            },
        )
    }

    editingMetadataLesson?.let { lesson ->
        EditLessonMetadataDialog(
            lesson = lesson,
            subjects = subjects,
            availableLabels = availableLabels,
            onRequestAddCategory = openAddCategoryDialog,
            onDismiss = { editingMetadataLesson = null },
            onConfirm = { title, desc, subjectId, labels ->
                viewModel.updateLessonMetadata(lesson.id, title, desc, subjectId, labels)
                editingMetadataLesson = null
            },
        )
    }

    deletingLesson?.let { lesson ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_lesson_title),
            message = stringResource(R.string.delete_lesson_message),
            onDismiss = { deletingLesson = null },
            onConfirm = {
                viewModel.deleteLesson(lesson)
                deletingLesson = null
            },
        )
    }

    if (showSubjectEditor) {
        SubjectEditorDialog(
            existingSubject = editingSubject,
            onDismiss = { showSubjectEditor = false },
            onConfirm = { name, colorHex, iconKey ->
                val target = editingSubject
                if (target == null) {
                    viewModel.addSubject(name, colorHex, iconKey)
                } else {
                    viewModel.updateSubject(
                        target.copy(name = name.trim(), colorHex = colorHex, iconKey = iconKey),
                    )
                }
                showSubjectEditor = false
            },
        )
    }

    deletingSubject?.let { subject ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_subject_title),
            message = stringResource(R.string.delete_subject_message),
            onDismiss = { deletingSubject = null },
            onConfirm = {
                viewModel.deleteSubject(subject)
                deletingSubject = null
            },
        )
    }

    renamingTag?.let { tag ->
        RenameTagDialog(
            currentTag = tag,
            onDismiss = { renamingTag = null },
            onConfirm = { newName ->
                viewModel.renameTag(tag, newName)
                renamingTag = null
            },
        )
    }

    deletingTag?.let { tag ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_tag_title),
            message = stringResource(R.string.delete_tag_message),
            onDismiss = { deletingTag = null },
            onConfirm = {
                viewModel.deleteTag(tag)
                deletingTag = null
            },
        )
    }
}
