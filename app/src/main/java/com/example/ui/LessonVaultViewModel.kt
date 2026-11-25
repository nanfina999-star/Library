package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LessonEntity
import com.example.data.local.LessonType
import com.example.data.local.SubjectEntity
import com.example.data.local.labelList
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.InMemoryThemePreferencesRepository
import com.example.data.preferences.ThemePreferencesRepository
import com.example.data.repository.LessonRepository
import com.example.data.storage.ImportedVaultFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class LessonSortOrder(val label: String) {
    DATE_CREATED("ថ្ងៃបង្កើត"),
    NAME("ឈ្មោះ"),
    CATEGORY("ប្រភេទ"),
}

data class VaultStats(
    val totalLessons: Int = 0,
    val totalSubjects: Int = 0,
    val totalBytes: Long = 0L,
    val pinnedCount: Int = 0,
)

class LessonVaultViewModel(
    private val repository: LessonRepository,
    private val themePreferences: ThemePreferencesRepository = InMemoryThemePreferencesRepository(),
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val soundEffectsEnabled: StateFlow<Boolean> = themePreferences.soundEffectsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val subjects: StateFlow<List<SubjectEntity>> = repository.subjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLessons: StateFlow<List<LessonEntity>> = repository.allLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedLessons: StateFlow<List<LessonEntity>> = repository.pinnedLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<LessonType?>(null)
    val selectedTypeFilter: StateFlow<LessonType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow<Long?>(null)
    val selectedSubjectFilter: StateFlow<Long?> = _selectedSubjectFilter.asStateFlow()

    private val _selectedLabelFilter = MutableStateFlow<String?>(null)
    val selectedLabelFilter: StateFlow<String?> = _selectedLabelFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(LessonSortOrder.DATE_CREATED)
    val sortOrder: StateFlow<LessonSortOrder> = _sortOrder.asStateFlow()

    private val _stagedImport = MutableStateFlow<ImportedVaultFile?>(null)
    val stagedImport: StateFlow<ImportedVaultFile?> = _stagedImport.asStateFlow()

    private val _preselectedSubjectIdForAdd = MutableStateFlow<Long?>(null)
    val preselectedSubjectIdForAdd: StateFlow<Long?> = _preselectedSubjectIdForAdd.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val availableLabels: StateFlow<List<String>> = combine(allLessons) { (lessons) ->
        lessons.flatMap { it.labelList() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labelCounts: StateFlow<Map<String, Int>> = combine(allLessons) { (lessons) ->
        val canonicalNames = mutableMapOf<String, String>()
        val counts = mutableMapOf<String, Int>()
        lessons.forEach { lesson ->
            lesson.labelList().forEach { label ->
                val key = label.lowercase()
                val display = canonicalNames.getOrPut(key) { label }
                counts[display] = (counts[display] ?: 0) + 1
            }
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categoryCounts: StateFlow<Map<Long, Int>> = combine(allLessons) { (lessons) ->
        lessons.groupingBy { it.subjectId }.eachCount()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private data class FilterCriteria(
        val query: String,
        val type: LessonType?,
        val subjectId: Long?,
        val label: String?,
        val sort: LessonSortOrder,
    )

    private val filterCriteria = combine(
        searchQuery,
        selectedTypeFilter,
        selectedSubjectFilter,
        selectedLabelFilter,
        sortOrder,
    ) { query, typeFilter, subjectFilter, labelFilter, sort ->
        FilterCriteria(query, typeFilter, subjectFilter, labelFilter, sort)
    }

    val filteredLessons: StateFlow<List<LessonEntity>> = combine(
        allLessons,
        subjects,
        filterCriteria,
    ) { lessons, subjectList, criteria ->
        val subjectNameMap = subjectList.associate { it.id to it.name.lowercase() }
        val trimmed = criteria.query.trim().lowercase()
        val filtered = lessons.filter { lesson ->
            val matchesType = criteria.type == null || lesson.type == criteria.type
            val matchesSubject = criteria.subjectId == null || lesson.subjectId == criteria.subjectId
            val matchesLabel = criteria.label == null || lesson.labelList().any {
                it.equals(criteria.label, ignoreCase = true)
            }
            val matchesQuery = trimmed.isEmpty() ||
                lesson.title.lowercase().contains(trimmed) ||
                lesson.description.lowercase().contains(trimmed) ||
                lesson.noteContent.lowercase().contains(trimmed) ||
                lesson.labels.lowercase().contains(trimmed) ||
                (lesson.originalFileName?.lowercase()?.contains(trimmed) == true)
            matchesType && matchesSubject && matchesLabel && matchesQuery
        }
        when (criteria.sort) {
            LessonSortOrder.DATE_CREATED -> filtered.sortedWith(
                compareByDescending<LessonEntity> { it.createdAt }
                    .thenBy { it.title.lowercase() },
            )
            LessonSortOrder.NAME -> filtered.sortedWith(
                compareBy<LessonEntity> { it.title.lowercase() }
                    .thenByDescending { it.createdAt },
            )
            LessonSortOrder.CATEGORY -> filtered.sortedWith(
                compareBy<LessonEntity> { subjectNameMap[it.subjectId].orEmpty() }
                    .thenBy { it.title.lowercase() }
                    .thenByDescending { it.createdAt },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultStats: StateFlow<VaultStats> = combine(
        allLessons,
        subjects,
    ) { lessons, subjectList ->
        VaultStats(
            totalLessons = lessons.size,
            totalSubjects = subjectList.size,
            totalBytes = lessons.sumOf { it.fileSizeBytes },
            pinnedCount = lessons.count { it.isPinned },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultStats())

    init {
        viewModelScope.launch {
            repository.ensureDefaultSubjects()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: LessonType?) {
        _selectedTypeFilter.value = if (_selectedTypeFilter.value == type) null else type
    }

    fun setSubjectFilter(subjectId: Long?) {
        _selectedSubjectFilter.value = if (_selectedSubjectFilter.value == subjectId) null else subjectId
    }

    fun setLabelFilter(label: String?) {
        _selectedLabelFilter.value = if (_selectedLabelFilter.value.equals(label, ignoreCase = true) && label != null) {
            null
        } else {
            label
        }
    }

    fun selectTagForLibrary(label: String) {
        _searchQuery.value = ""
        _selectedTypeFilter.value = null
        _selectedSubjectFilter.value = null
        _selectedLabelFilter.value = label
    }

    fun setSortOrder(order: LessonSortOrder) {
        _sortOrder.value = order
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedTypeFilter.value = null
        _selectedSubjectFilter.value = null
        _selectedLabelFilter.value = null
    }

    fun setPreselectedSubjectForAdd(subjectId: Long?) {
        _preselectedSubjectIdForAdd.value = subjectId
    }

    fun onUriPicked(uri: Uri, isImage: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                repository.stageImportFromUri(uri, forceImage = isImage)
            }.onSuccess { staged ->
                _stagedImport.value = staged
            }.onFailure {
                _events.tryEmit("មិនអាចនាំចូលឯកសារនេះបានទេ")
            }
            _isBusy.value = false
        }
    }

    fun cancelStagedImport() {
        val current = _stagedImport.value ?: return
        _stagedImport.value = null
        repository.discardStagedImport(current)
    }

    fun commitStagedImport(
        title: String,
        description: String,
        subjectId: Long,
        studyNote: String,
        labels: List<String> = emptyList(),
        onSaved: (Long) -> Unit = {},
    ) {
        val current = _stagedImport.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                repository.commitImportedLesson(
                    staged = current,
                    title = title,
                    description = description,
                    subjectId = subjectId,
                    studyNote = studyNote,
                    labels = labels,
                )
            }.onSuccess { newId ->
                _stagedImport.value = null
                _events.tryEmit("បានរក្សាទុកឯកសារមេរៀនក្នុងកម្មវិធីរួចរាល់")
                onSaved(newId)
            }.onFailure {
                _events.tryEmit("មានបញ្ហាក្នុងការរក្សាទុកឯកសារ")
            }
            _isBusy.value = false
        }
    }

    fun saveNoteLesson(
        lessonId: Long?,
        title: String,
        description: String,
        subjectId: Long,
        noteContent: String,
        labels: List<String> = emptyList(),
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val savedId = repository.saveNoteLesson(
                lessonId = lessonId,
                title = title,
                description = description,
                subjectId = subjectId,
                noteContent = noteContent,
                labels = labels,
            )
            _events.tryEmit("បានរក្សាទុកកំណត់ត្រាមេរៀនរួចរាល់")
            onSaved(savedId)
        }
    }

    fun updateLessonMetadata(
        lessonId: Long,
        title: String,
        description: String,
        subjectId: Long,
        labels: List<String>,
    ) {
        viewModelScope.launch {
            repository.updateLessonMetadata(lessonId, title, description, subjectId, labels)
            _events.tryEmit("បានកែប្រែព័ត៌មានមេរៀន")
        }
    }

    fun updateLessonCategoryAndLabels(
        lessonId: Long,
        subjectId: Long,
        labels: List<String>,
    ) {
        viewModelScope.launch {
            repository.updateLessonCategoryAndLabels(lessonId, subjectId, labels)
            _events.tryEmit("បានធ្វើបច្ចុប្បន្នភាពប្រភេទ និងស្លាក")
        }
    }

    fun renameTag(oldLabel: String, newLabel: String) {
        val cleanNew = newLabel.trim().replace(",", "")
        if (cleanNew.isBlank()) return
        viewModelScope.launch {
            val count = repository.renameLabelAcrossLessons(oldLabel, cleanNew)
            if (_selectedLabelFilter.value.equals(oldLabel, ignoreCase = true)) {
                _selectedLabelFilter.value = cleanNew
            }
            if (count > 0) {
                _events.tryEmit("បានកែឈ្មោះស្លាកទៅជា «$cleanNew»")
            }
        }
    }

    fun deleteTag(label: String) {
        viewModelScope.launch {
            val count = repository.removeLabelFromAllLessons(label)
            if (_selectedLabelFilter.value.equals(label, ignoreCase = true)) {
                _selectedLabelFilter.value = null
            }
            if (count > 0) {
                _events.tryEmit("បានដកស្លាក «$label» ចេញពីឯកសារ")
            }
        }
    }

    fun togglePinned(lesson: LessonEntity) {
        viewModelScope.launch {
            repository.togglePinned(lesson)
            val msg = if (!lesson.isPinned) "បានដាក់ម្ជុលជាមេរៀនសំខាន់" else "បានដកចេញពីមេរៀនសំខាន់"
            _events.tryEmit(msg)
        }
    }

    fun deleteLesson(lesson: LessonEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteLesson(lesson)
            _events.tryEmit("បានលុបឯកសារមេរៀន")
            onDeleted()
        }
    }

    fun addSubject(name: String, colorHex: Long, iconKey: String, onAdded: (Long) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addSubject(name, colorHex, iconKey)
            _events.tryEmit("បានបង្កើតមុខវិជ្ជាថ្មី")
            onAdded(id)
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        if (subject.name.isBlank()) return
        viewModelScope.launch {
            repository.updateSubject(subject)
            _events.tryEmit("បានកែប្រែមុខវិជ្ជា")
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            val fallback = subjects.value.firstOrNull { it.id != subject.id }?.id
            repository.deleteSubject(subject.id, fallback)
            if (_selectedSubjectFilter.value == subject.id) {
                _selectedSubjectFilter.value = null
            }
            _events.tryEmit("បានលុបមុខវិជ្ជា")
        }
    }

    fun observeLesson(lessonId: Long): Flow<LessonEntity?> = repository.observeLesson(lessonId)

    fun observeLessonsBySubject(subjectId: Long): Flow<List<LessonEntity>> =
        repository.observeLessonsBySubject(subjectId)

    fun updateLastPage(lessonId: Long, page: Int) {
        viewModelScope.launch {
            repository.updateLastPage(lessonId, page)
        }
    }

    fun updateStudyNotes(lessonId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateStudyNotes(lessonId, notes)
            _events.tryEmit("បានរក្សាទុកកំណត់ត្រាសិក្សា")
        }
    }

    fun resolveFile(relativePath: String?): File? = repository.resolveLessonFile(relativePath)

    fun getShareUri(relativePath: String?) = repository.getShareUri(relativePath)

    suspend fun readTextFile(relativePath: String?): String = repository.readStoredText(relativePath)

    fun toggleTheme(currentlyDark: Boolean) {
        viewModelScope.launch {
            themePreferences.toggleLightDark(currentlyDark)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun toggleSoundEffects(currentlyEnabled: Boolean) {
        viewModelScope.launch {
            themePreferences.toggleSoundEffects(currentlyEnabled)
        }
    }
}

class LessonVaultViewModelFactory(
    private val repository: LessonRepository,
    private val themePreferences: ThemePreferencesRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LessonVaultViewModel::class.java)) {
            return LessonVaultViewModel(repository, themePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
