package com.glyph.launcher.ui.main

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.data.repository.GameRepository
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.util.InputHandler
import com.glyph.launcher.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val FILTER_FAVORITES = "FILTER_FAVORITES"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: GameRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // ── UI State ─────────────────────────────────────────────────────────────

    /** Bar chip index: 0=ALL, 1..n=platforms, n+1=SETTINGS, n+2=BACK. L1/R1 cycle; A confirms. */
    data class UiState(
        val games: List<GameEntity> = emptyList(),
        val selectedIndex: Int = 0,
        val platforms: List<String> = emptyList(),
        val activePlatformFilter: String? = null,     // null = "All"
        val isLoading: Boolean = true,
        val isEmpty: Boolean = false,
        val barSelectionIndex: Int = 0,              // 0=ALL, 1..size=platform, size+1=SETTINGS, size+2=BACK
        val transitionDirection: Int = 0,            // 1=Forward (Slide Left), -1=Backward (Slide Right)
        val searchQuery: String = "",
        val isSearchActive: Boolean = false
    ) {
        val selectedGame: GameEntity? get() = games.getOrNull(selectedIndex)
        fun isSettingsSelected(): Boolean = when {
            platforms.isEmpty() -> barSelectionIndex == 0
            else -> barSelectionIndex == platforms.size + 2
        }
        fun isBackSelected(): Boolean = when {
            platforms.isEmpty() -> barSelectionIndex == 1
            else -> barSelectionIndex == platforms.size + 3
        }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Events (one-shot) ────────────────────────────────────────────────────

    sealed class Event {
        data class LaunchGame(val game: GameEntity) : Event()
        data class ShowEmulatorPicker(val game: GameEntity, val platform: Platform) : Event()
        data class ShowRomOptions(val game: GameEntity) : Event()
        object NavigateToSetup : Event()
        object GoBack : Event()
        object ToggleSearch : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    // ── Internal State ───────────────────────────────────────────────────────

    private val _platformFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    init {
        observeGames()
        restoreLastPlatformFilter()
    }

    private fun observeGames() {
        viewModelScope.launch {
            combine(
                repository.getAllGames(),
                repository.getDistinctPlatforms(),
                _platformFilter,
                _searchQuery
            ) { allGames, platforms, filter, query ->
                val platformFiltered = when {
                    filter == FILTER_FAVORITES -> allGames.filter { it.isFavorite }
                    filter != null -> allGames.filter { it.platformTag == filter }
                    else -> allGames
                }

                val filtered = if (query.isNotBlank()) {
                    platformFiltered.filter {
                        it.displayTitle.contains(query, ignoreCase = true) ||
                        it.filename.contains(query, ignoreCase = true)
                    }
                } else {
                    platformFiltered
                }

                _uiState.update { current ->
                    val newIndex = current.selectedIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))
                    val barMax = if (platforms.isEmpty()) 1 else platforms.size + 3 // ALL, FAV, [platforms], SETTINGS, BACK
                    val syncedBarIndex = when {
                        current.barSelectionIndex !in 0..barMax -> 0
                        platforms.isEmpty() -> current.barSelectionIndex
                        filter == FILTER_FAVORITES -> 1
                        filter != null && filter in platforms -> platforms.indexOf(filter) + 2
                        current.barSelectionIndex == platforms.size + 2 -> platforms.size + 2 // SETTINGS
                        current.barSelectionIndex == platforms.size + 3 -> platforms.size + 3 // BACK
                        else -> 0 // ALL or invalid
                    }
                    current.copy(
                        games = filtered,
                        platforms = platforms,
                        activePlatformFilter = filter,
                        selectedIndex = newIndex,
                        isLoading = false,
                        isEmpty = filtered.isEmpty(),
                        barSelectionIndex = syncedBarIndex,
                        searchQuery = query,
                        isSearchActive = query.isNotBlank()
                    )
                }
            }.collect { /* combined flow drives state updates above */ }
        }
    }

    private fun restoreLastPlatformFilter() {
        viewModelScope.launch {
            val lastFilter = preferencesManager.lastPlatformFilter.first()
            _platformFilter.value = lastFilter
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ── Input Handling ───────────────────────────────────────────────────────

    /**
     * Handle a hardware key event from the controller.
     * Returns true if the event was consumed.
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        val action = InputHandler.mapKeyEvent(event)
        if (action == InputHandler.Action.NONE) return false

        when (action) {
            InputHandler.Action.NAVIGATE_DOWN -> moveSelection(1)
            InputHandler.Action.NAVIGATE_UP -> moveSelection(-1)
            InputHandler.Action.CONFIRM -> {
                val state = _uiState.value
                when {
                    state.isSettingsSelected() -> { _events.tryEmit(Event.NavigateToSetup); return true }
                    state.isBackSelected() -> { _events.tryEmit(Event.GoBack); return true }
                    else -> confirmSelection()
                }
            }
            InputHandler.Action.BACK -> { /* Let system handle back */ return false }
            InputHandler.Action.OPTIONS -> {
                _uiState.value.selectedGame?.let { _events.tryEmit(Event.ShowRomOptions(it)) }
            }
            InputHandler.Action.TRIGGER_RIGHT -> cyclePlatformSelection(forward = true)
            InputHandler.Action.TRIGGER_LEFT -> cyclePlatformSelection(forward = false)
            InputHandler.Action.MENU -> {
                _events.tryEmit(Event.NavigateToSetup)
            }
            InputHandler.Action.SEARCH -> {
                _events.tryEmit(Event.ToggleSearch)
            }
            InputHandler.Action.TOGGLE_FAVORITE -> {
                _uiState.value.selectedGame?.let { toggleFavorite(it) }
            }
            else -> return false
        }
        return true
    }

    /**
     * Move the selection index by delta (+1 = down, -1 = up).
     */
    fun moveSelection(delta: Int) {
        _uiState.update { current ->
            if (current.games.isEmpty()) return@update current
            val newIndex = (current.selectedIndex + delta)
                .coerceIn(0, current.games.size - 1)
            current.copy(selectedIndex = newIndex)
        }
    }

    /**
     * Select a game by index (e.g., from touch tap).
     */
    fun selectIndex(index: Int) {
        _uiState.update { current ->
            if (index in current.games.indices) {
                current.copy(selectedIndex = index)
            } else current
        }
    }

    /**
     * Confirm the current selection — launch the game.
     */
    fun confirmSelection() {
        val game = _uiState.value.selectedGame ?: return
        val platform = Platform.fromTag(game.platformTag) ?: return

        viewModelScope.launch {
            // Check if there's a preferred emulator for this platform
            val preferredPackage = game.preferredEmulatorPackage
                ?: preferencesManager.getEmulatorPreference(game.platformTag).first()

            if (preferredPackage != null) {
                repository.recordPlay(game.gameId)
                _events.emit(Event.LaunchGame(game.copy(preferredEmulatorPackage = preferredPackage)))
            } else {
                // Need to show emulator picker
                _events.emit(Event.ShowEmulatorPicker(game, platform))
            }
        }
    }

    /** Cycle bar: ALL -> FAV -> platforms -> SETTINGS -> BACK. */
    fun cyclePlatformSelection(forward: Boolean) {
        val state = _uiState.value
        val platforms = state.platforms
        val total = if (platforms.isEmpty()) 2 else platforms.size + 4  // empty: SETTINGS, BACK; else ALL, FAV, [platforms], SETTINGS, BACK
        val next = if (forward) {
            (state.barSelectionIndex + 1) % total
        } else {
            (state.barSelectionIndex - 1 + total) % total
        }
        val direction = if (forward) 1 else -1
        _uiState.update { it.copy(barSelectionIndex = next, transitionDirection = direction) }
        
        if (platforms.isEmpty()) return // Only SETTINGS/BACK logic if empty, handled by confirmSelection or click? No, Confirm handles action.
        
        when (next) {
            0 -> {
                _platformFilter.value = null
                viewModelScope.launch { preferencesManager.setLastPlatformFilter(null) }
            }
            1 -> {
                _platformFilter.value = FILTER_FAVORITES
                viewModelScope.launch { preferencesManager.setLastPlatformFilter(FILTER_FAVORITES) }
            }
            in 2..platforms.size + 1 -> {
                val filter = platforms[next - 2]
                _platformFilter.value = filter
                viewModelScope.launch { preferencesManager.setLastPlatformFilter(filter) }
            }
            else -> { /* SETTINGS or BACK */ }
        }
    }

    /**
     * Set platform filter directly (e.g. tap on chip).
     */
    fun setPlatformFilter(platformTag: String?) {
        val state = _uiState.value
        _platformFilter.value = platformTag
        
        val barIndex = when {
            platformTag == null -> 0
            platformTag == FILTER_FAVORITES -> 1
            else -> (state.platforms.indexOf(platformTag) + 2).coerceIn(0, state.platforms.size + 3)
        }
        
        _uiState.update { it.copy(barSelectionIndex = barIndex) }
        viewModelScope.launch {
            preferencesManager.setLastPlatformFilter(platformTag)
        }
    }

    /**
     * Set bar selection by index (e.g. when user focuses a chip with D-pad).
     * Syncs platform filter when index is ALL or a platform chip.
     */
    fun setBarSelectionIndex(index: Int) {
        val state = _uiState.value
        val platforms = state.platforms
        val maxIndex = if (platforms.isEmpty()) 1 else platforms.size + 3
        val safe = index.coerceIn(0, maxIndex)
        _uiState.update { it.copy(barSelectionIndex = safe) }
        
        if (platforms.isEmpty()) return
        
        when (safe) {
            0 -> {
                _platformFilter.value = null
                viewModelScope.launch { preferencesManager.setLastPlatformFilter(null) }
            }
            1 -> {
                _platformFilter.value = FILTER_FAVORITES
                viewModelScope.launch { preferencesManager.setLastPlatformFilter(FILTER_FAVORITES) }
            }
            in 2..platforms.size + 1 -> {
                val filter = platforms[safe - 2]
                _platformFilter.value = filter
                viewModelScope.launch { preferencesManager.setLastPlatformFilter(filter) }
            }
            else -> { /* SETTINGS or BACK */ }
        }
    }

    /**
     * Record that a game was launched with a specific emulator.
     * Optionally save as the default for this platform.
     */
    /**
     * Record emulator choice and launch. Always save as default for this platform
     * so the user doesn't have to select again next time.
     */
    fun onEmulatorSelected(
        game: GameEntity,
        emulatorPackage: String,
        alwaysUse: Boolean
    ) {
        viewModelScope.launch {
            preferencesManager.setEmulatorPreference(game.platformTag, emulatorPackage)
            repository.recordPlay(game.gameId)
            _events.emit(Event.LaunchGame(game.copy(preferredEmulatorPackage = emulatorPackage)))
        }
    }

    fun rescrapeGame(game: GameEntity) {
        viewModelScope.launch {
            repository.scrapeGame(game)
        }
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch {
            repository.deleteGame(game.gameId)
        }
    }

    fun updateGameTitle(gameId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateGameTitle(gameId, newTitle)
        }
    }

    fun setGameCoverPath(gameId: Long, imagePath: String?) {
        viewModelScope.launch {
            repository.updateGameCoverPath(gameId, imagePath)
        }
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch {
            repository.updateFavorite(game.gameId, !game.isFavorite)
        }
    }
}
