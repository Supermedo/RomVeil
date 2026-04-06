package com.glyph.launcher.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Progress for one scrape run. */
data class ScrapingProgress(
    val current: Int,
    val total: Int,
    val currentTitle: String
)

/** Live state of the scraper (survives leaving Settings). */
data class ScrapingState(
    val isScraping: Boolean,
    val progress: ScrapingProgress?
)

/**
 * Runs scraping in application scope so it continues when the user leaves Settings.
 * Exposes state and supports stop.
 */
@Singleton
class ScrapingController @Inject constructor(
    private val repository: GameRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scrapeJob: Job? = null

    private val _state = MutableStateFlow(ScrapingState(isScraping = false, progress = null))
    val state: StateFlow<ScrapingState> = _state.asStateFlow()

    /** Start scraping PENDING games. Runs in app scope; survives navigation. */
    fun startScraping() {
        scrapeJob?.cancel()
        _state.value = ScrapingState(isScraping = true, progress = ScrapingProgress(0, 0, "Starting…"))
        scrapeJob = scope.launch {
            try {
                repository.scrapeAllPending { current, total, title ->
                    _state.value = ScrapingState(
                        isScraping = true,
                        progress = ScrapingProgress(current, total, title)
                    )
                }
            } finally {
                _state.value = ScrapingState(isScraping = false, progress = null)
            }
        }
    }

    /** Reset all games to PENDING then start scraping. */
    fun resetAndStartScraping() {
        scrapeJob?.cancel()
        _state.value = ScrapingState(isScraping = true, progress = ScrapingProgress(0, 0, "Resetting…"))
        scrapeJob = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.resetAllScrapeStatusToPending()
                }
                _state.value = ScrapingState(isScraping = true, progress = ScrapingProgress(0, 0, "Starting…"))
                repository.scrapeAllPending { current, total, title ->
                    _state.value = ScrapingState(
                        isScraping = true,
                        progress = ScrapingProgress(current, total, title)
                    )
                }
            } finally {
                _state.value = ScrapingState(isScraping = false, progress = null)
            }
        }
    }

    /** Stop the current scrape. */
    fun stopScraping() {
        scrapeJob?.cancel()
        scrapeJob = null
        _state.value = ScrapingState(isScraping = false, progress = null)
    }
}
