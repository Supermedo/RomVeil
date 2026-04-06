package com.glyph.launcher.ui.setup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyph.launcher.data.repository.GameRepository
import com.glyph.launcher.data.repository.ScrapingController
import com.glyph.launcher.domain.model.EmulatorInfo
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.util.EmulatorLauncher
import com.glyph.launcher.util.FolderMapping
import com.glyph.launcher.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val repository: GameRepository,
    private val scrapingController: ScrapingController
) : ViewModel() {

    enum class Step {
        WELCOME,
        FOLDER_MANAGEMENT,
        EMULATORS,
        SCRAPER_API,
        SCANNING,
        SCAN_COMPLETE,
        SCRAPING,
        DONE
    }

    /** Per-platform emulator state for setup: installed list + selected default. */
    data class EmulatorStepState(
        val platform: Platform,
        val installed: List<EmulatorInfo>,
        val preferredPackage: String?
    )

    data class ScanProgress(
        val currentFolder: String,
        val currentFile: String,
        val scannedCount: Int
    )

    data class ScrapeProgress(
        val current: Int,
        val total: Int,
        val currentTitle: String
    )

    data class UiState(
        val step: Step = Step.WELCOME,
        val folderMappings: List<FolderMapping> = emptyList(),
        val emulatorSteps: List<EmulatorStepState> = emptyList(),
        val defaultScraper: String = "thegamesdb",
        val theGamesDbApiKey: String = "",
        val rawgApiKey: String = "",
        val mobyGamesApiKey: String = "",
        val error: String? = null,
        val scanProgress: ScanProgress? = null,
        val totalDiscovered: Int = 0,
        val scrapeProgress: ScrapeProgress? = null,
        val showPlatformPicker: Boolean = false,
        val pendingFolderName: String? = null,
        val pendingFolderUri: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.folderMappings.collect { mappings ->
                _uiState.update { it.copy(folderMappings = mappings) }
            }
        }
        viewModelScope.launch {
            var wasScraping = false
            scrapingController.state.collect { s ->
                _uiState.update { state ->
                    state.copy(
                        scrapeProgress = s.progress?.let { p ->
                            ScrapeProgress(p.current, p.total, p.currentTitle)
                        }
                    )
                }
                if (wasScraping && !s.isScraping) {
                    _uiState.update { it.copy(step = Step.DONE) }
                    preferencesManager.setSetupComplete(true)
                }
                wasScraping = s.isScraping
            }
        }
    }

    fun nextFromWelcome() {
        _uiState.update { it.copy(step = Step.FOLDER_MANAGEMENT) }
    }

    fun nextFromFolders() {
        val mappings = _uiState.value.folderMappings
        if (mappings.isEmpty()) {
            _uiState.update { it.copy(error = "Add at least one folder") }
            return
        }
        _uiState.update { it.copy(error = null) }
        loadEmulatorStepsAndGoToEmulators()
    }

    private fun loadEmulatorStepsAndGoToEmulators() {
        viewModelScope.launch {
            val mappings = _uiState.value.folderMappings
            val platforms = mappings.map { it.platformTag }.distinct()
            val steps = withContext(Dispatchers.IO) {
                platforms.mapNotNull { tag ->
                    val platform = Platform.fromTag(tag) ?: return@mapNotNull null
                    val detection = EmulatorLauncher.detectInstalledEmulators(context, platform)
                    val preferred = preferencesManager.getEmulatorPreference(tag).first()?.takeIf { it.isNotBlank() }
                    EmulatorStepState(
                        platform = platform,
                        installed = detection.installed,
                        preferredPackage = preferred
                    )
                }
            }
            _uiState.update {
                it.copy(step = Step.EMULATORS, emulatorSteps = steps)
            }
        }
    }

    fun nextFromEmulators() {
        loadScraperPrefsAndGoToScraperApi()
    }

    private fun loadScraperPrefsAndGoToScraperApi() {
        viewModelScope.launch {
            val defaultScraper = preferencesManager.defaultScraper.first()
            val tgdb = preferencesManager.theGamesDbApiKey.first()
            val rawg = preferencesManager.rawgApiKey.first()
            val moby = preferencesManager.mobyGamesApiKey.first()
            _uiState.update {
                it.copy(
                    step = Step.SCRAPER_API,
                    defaultScraper = defaultScraper,
                    theGamesDbApiKey = tgdb,
                    rawgApiKey = rawg,
                    mobyGamesApiKey = moby
                )
            }
        }
    }

    fun nextFromScraperApi() {
        _uiState.update { it.copy(step = Step.SCANNING, error = null, scanProgress = ScanProgress("", "", 0)) }
        startScanning()
    }

    fun setEmulatorPreference(platformTag: String, packageName: String) {
        viewModelScope.launch {
            preferencesManager.setEmulatorPreference(platformTag, packageName)
            _uiState.update { state ->
                state.copy(
                    emulatorSteps = state.emulatorSteps.map {
                        if (it.platform.tag == platformTag) it.copy(preferredPackage = packageName) else it
                    }
                )
            }
        }
    }

    fun clearEmulatorPreference(platformTag: String) {
        viewModelScope.launch {
            preferencesManager.setEmulatorPreference(platformTag, "")
            _uiState.update { state ->
                state.copy(
                    emulatorSteps = state.emulatorSteps.map {
                        if (it.platform.tag == platformTag) it.copy(preferredPackage = null) else it
                    }
                )
            }
        }
    }

    fun setDefaultScraper(scraper: String) {
        viewModelScope.launch {
            preferencesManager.setDefaultScraper(scraper)
            _uiState.update { it.copy(defaultScraper = scraper) }
        }
    }

    fun updateTheGamesDbApiKey(value: String) {
        _uiState.update { it.copy(theGamesDbApiKey = value) }
    }

    fun saveTheGamesDbApiKey() {
        viewModelScope.launch {
            preferencesManager.setTheGamesDbApiKey(_uiState.value.theGamesDbApiKey)
        }
    }

    fun updateRawgApiKey(value: String) {
        _uiState.update { it.copy(rawgApiKey = value) }
    }

    fun saveRawgApiKey() {
        viewModelScope.launch {
            preferencesManager.setRawgApiKey(_uiState.value.rawgApiKey)
        }
    }

    fun updateMobyGamesApiKey(value: String) {
        _uiState.update { it.copy(mobyGamesApiKey = value) }
    }

    fun saveMobyGamesApiKey() {
        viewModelScope.launch {
            preferencesManager.setMobyGamesApiKey(_uiState.value.mobyGamesApiKey)
        }
    }

    fun onFolderPicked(uri: Uri, folderName: String) {
        _uiState.update {
            it.copy(
                showPlatformPicker = true,
                pendingFolderName = folderName,
                pendingFolderUri = uri.toString(),
                error = null
            )
        }
    }

    fun onPlatformChosen(platformTag: String) {
        val uri = _uiState.value.pendingFolderUri ?: return
        val folderName = _uiState.value.pendingFolderName ?: return
        viewModelScope.launch {
            preferencesManager.addFolderMapping(FolderMapping(uri, platformTag, folderName))
            _uiState.update {
                it.copy(
                    showPlatformPicker = false,
                    pendingFolderName = null,
                    pendingFolderUri = null
                )
            }
        }
    }

    fun dismissPlatformPicker() {
        _uiState.update {
            it.copy(showPlatformPicker = false, pendingFolderName = null, pendingFolderUri = null)
        }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch {
            preferencesManager.removeFolderMapping(uri)
        }
    }

    fun startScanning() {
        val mappings = _uiState.value.folderMappings
        if (mappings.isEmpty()) {
            _uiState.update { it.copy(error = "Add at least one folder") }
            return
        }
        _uiState.update {
            it.copy(step = Step.SCANNING, error = null, scanProgress = ScanProgress("", "", 0))
        }
        viewModelScope.launch {
            var totalSoFar = 0
            for (mapping in mappings) {
                val platform = Platform.fromTag(mapping.platformTag)
                val treeUri = Uri.parse(mapping.uri)
                val result = withContext(Dispatchers.IO) {
                    repository.scanDirectory(treeUri, platform, onProgress = { count, currentFile ->
                        _uiState.update {
                            it.copy(
                                scanProgress = ScanProgress(
                                    currentFolder = mapping.folderName,
                                    currentFile = currentFile,
                                    scannedCount = totalSoFar + count
                                )
                            )
                        }
                    })
                }
                totalSoFar += result.discovered.size
            }
            _uiState.update {
                it.copy(
                    step = Step.SCAN_COMPLETE,
                    totalDiscovered = totalSoFar,
                    scanProgress = null
                )
            }
        }
    }

    fun startScraping() {
        _uiState.update { it.copy(step = Step.SCRAPING) }
        scrapingController.startScraping()
    }

    fun skipScraping() {
        viewModelScope.launch {
            preferencesManager.setSetupComplete(true)
            _uiState.update { it.copy(step = Step.DONE) }
        }
    }
}
